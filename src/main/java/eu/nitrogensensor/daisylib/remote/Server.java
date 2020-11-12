package eu.nitrogensensor.daisylib.remote;

import com.google.gson.Gson;
import eu.nitrogensensor.daisylib.ExecutionCache;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.Utils;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.javalin.plugin.json.JavalinJson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Server {
    private static final boolean USIKKER_KØR = false;
    private static boolean PAK_UD_VED_MODTAGELSEN = true;
    private static Javalin app;
    private static String url;

    private static CloudStorageArbejdsfiler cloudStorageArbejdsfiler = new CloudStorageArbejdsfiler();

    //System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
    //static { System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT %4$s %2$s %5$s%6$s%n"); }
    static { System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT %5$s%6$s%n"); }
    static Logger log = Logger.getGlobal();
    static { log.setLevel( Level.ALL ); log.getParent().getHandlers()[0].setLevel(Level.ALL); } // vis alt log


    public static String getUrl() {
        return url;
    }

    public static void main(String[] args) {
        start();
    }

    public static void stop() {
        app.stop();
        app = null;
    }

    public static void start() {
        String port = System.getenv("PORT");
        if (port == null) port = "3210";
        start(Integer.parseInt(port));
    }

    public static void start(int port) {
        if (app!=null) return;

        Gson gson = MyGsonPathConverter.buildGson();
        JavalinJson.setFromJsonMapper(gson::fromJson);
        JavalinJson.setToJsonMapper(gson::toJson);

        url = "http://localhost:"+port;
        app = Javalin.create().start(port);
        /*
        app.before(ctx -> {
            // runs before all requests
            log.fine("Server "+ctx.method()+" på " +ctx.url());
        });
         */
        app.after(ctx -> {
            log.fine("Server "+ctx.method()+" på " +ctx.url()+" gav "+ctx.res.getStatus());
        });
        app.exception(Exception.class, (e, ctx) -> {
            log.warning(e.toString());
            e.printStackTrace();
        });
        app.get("/", ctx -> ctx.contentType("text/html").result("<html><body>Endpoints er skjulte. Du kan evt spoerge på <a href='json'>json</a>"));
        app.get("/json", ctx -> ctx.result("Hello World"));
        app.post("/uploadZip", ctx -> upload(ctx));
        if (USIKKER_KØR) app.get("/koer", ctx -> kørKommando(ctx, null));
        app.post("/sim", ctx -> sim(ctx));
    }

    private static String kørKommando(Context ctx, String[] parms) throws IOException, InterruptedException {
        if (!USIKKER_KØR) return "nej";
            String k = ctx.queryParam("k");
        if (parms==null && k!=null) {
            parms = k.split(" ");
        }
        if (parms==null) {
            ctx.contentType("text/html").result("<html><body><form action=koer method=get><input name=k type=text></form></html>");
            return "";
        }

        log.info("koer "+ Arrays.toString(parms));
        File f = new File("/tmp/koer.txt");
        f.delete();
        Process process = new ProcessBuilder(parms)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(f))
                .redirectError(ProcessBuilder.Redirect.appendTo(f))
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .start();
        int returkode = process.waitFor();
        process.destroy();
        String res = new String(Files.readAllBytes(f.toPath())) + "\nreturkode: "+returkode;
        if (ctx!=null) ctx.result(res);
        return res;
    }

    private static Path uploadMappe = Paths.get("upload");
    private static long uploadMappeSenesteId;

    private static String upload(Context ctx) throws IOException {
        Files.createDirectories(uploadMappe);

        Path denneUploadMappe;

        synchronized (uploadMappe) {
            long nu = System.currentTimeMillis();
            while (nu <= uploadMappeSenesteId) nu++;
            uploadMappeSenesteId = nu;
            denneUploadMappe = Files.createDirectory(uploadMappe.resolve(Long.toString(nu, Character.MAX_RADIX)));
        }

        String batchId = uploadMappe.relativize(denneUploadMappe).toString();
        log.fine("upload får batchId = " + batchId);

        UploadedFile file = ctx.uploadedFile("zipfil");
        if (file!=null) {
            InputStream is = file.getContent();
            if (PAK_UD_VED_MODTAGELSEN) {
                log.fine("Server upladZip " + file.getFilename()+" pakkes ud i "+denneUploadMappe);
                if (!is.markSupported()) throw new RuntimeException("is skal kunne spoles tilbage");
                is.mark(Integer.MAX_VALUE);
                Utils.unzipMappe(is, denneUploadMappe.toString());
                is.reset();
            }
            cloudStorageArbejdsfiler.gem(is, batchId);
        }


        ctx.html(batchId);
        return batchId;
    }


    private static String tjekSikkerSti(String sti0) {
        String sti = sti0;
        if (sti.startsWith("/")) sti = sti.substring(1); // fjern / i starten
        sti.replace("..", "");
        if (!sti.equals(sti0)) new IllegalArgumentException("Usikker sti "+sti0+" lavet om til "+sti).printStackTrace();
        return sti;
    }

    private static void sim(Context ctx) {
        ExtractedContent extractedContent = new ExtractedContent();
        try {
            //System.out.println("Server sim "+ctx.url());
            log.fine("Server sim " + ctx.body());
            ExecutionBatch batch = JavalinJson.fromJson(ctx.body(), ExecutionBatch.class);
            if (batch == null) log.fine("Ingen batch fra " + ctx.body());
            else {
                //ExecutionBatch batch = ctx.bodyAsClass(ExecutionBatch.class);
                batch.kørsel.setId(batch.oploadId+"_"+batch.kørsel.getId());
                batch.kørsel.directory = uploadMappe.resolve(tjekSikkerSti(batch.oploadId));
                if (Files.exists(batch.kørsel.directory) && batch.kørsel.directory.toFile().list().length > 0) {
                    //log.fine("Denne instans har allerede batch " + batch.kørsel.directory);
                    // +" med "+ Files.list(batch.kørsel.directory).collect(Collectors.toList())
                } else {
                    log.fine("Denne instans har ikke batch " + batch.kørsel.directory);

                    Path fil = cloudStorageArbejdsfiler.hent(batch.oploadId);
                    if (fil == null) throw new IllegalArgumentException("No such batch.oploadId " + batch.oploadId);
                    InputStream fis = Files.newInputStream(fil);
                    Utils.unzipMappe(fis, batch.kørsel.directory.toString());
                    fis.close();
                    Files.delete(fil);
                }
                //System.out.println("Server ExecutionBatch " + batch.oploadId);


                // Benyt en eksekveringscache - bemærk at batch.kørsel peger DIREKTE ned i cachen, så det er vigtigt at cachede kørselsmapper ikke ændres
                ExecutionCache executionCache = new ExecutionCache(Paths.get("../tmp/daisy-execution-cache/"));
                boolean varCachet = executionCache.udfyldFraCache(batch.kørsel);
                if (!varCachet) try {
                    // Vi skal have det over i en anden midlertidig mappe, ellers kan det være vi får knas med at
                    // evt andre samtidige kørsler skriver i de samme outputfiler
                    batch.kørsel.copyToDirectory(Files.createTempDirectory("tmpkørsel_" + batch.kørsel.getId().replaceAll("[^A-Za-z0-9_]", "_")+"_"));
                    //batch.resultExtractor.tjekResultatIkkeAlleredeFindes(batch.kørsel.directory); // burde egentlig ikke være nødvendigt, da det også tjekkes af klienten, men man kan ikke stole på klienter...
                    //log.fine("batch.kørsel.directory = " + batch.kørsel.directory);

                    // HER KØRES MODELLEN !!!!
                    batch.kørsel.run();

                    executionCache.gemICache(batch.kørsel);
                } catch (IOException e) {
                    e.printStackTrace();
                    extractedContent.exception = e;
                    //batch.resultExtractor.addFile("daisy.log");
                    //batch.resultExtractor.addFile("daisyErr.log");
                    batch.resultExtractor = new ResultExtractor().addFile("."); // Kopiér alt tilbage til klienten i tilfælde af en fejl
                }

                if (batch.resultExtractor==null) batch.resultExtractor = new ResultExtractor(); // standard er at trække alt ud....
                batch.resultExtractor.extractToHashMap(batch.kørsel.directory, extractedContent.fileContensMap);
                if (!varCachet)
                    Utils.sletMappe(batch.kørsel.directory); // ryd op - men KUN hvis det ikke kommer fra cachen!
            }
        } catch (Throwable e) {
            e.printStackTrace();
            extractedContent.exception = e;
        }
        ctx.json(extractedContent);
    }

}




/*
      Sende enkeltfiler fra klient:
    public static MultipartBody tilføjInputfilerTilRequest(MultipartBody oploadReq, Path inputDir) throws IOException {
        ArrayList<Path> filer = new ArrayList<Path>();
        Files.walk(inputDir).filter(fraFil -> !Files.isDirectory(fraFil)).forEach(f -> filer.add(f));
        if (FEJLFINDING) System.out.println("filer="+filer);
        for (Path fil : filer) {
            oploadReq = oploadReq.field("filer", fil.toFile(), inputDir.relativize(fil).toString());
        }
        return oploadReq;
    }

    Modtage enkeltfiler på serveren
        ctx.uploadedFiles("filer").forEach(file -> {
            try {
                System.out.println("Server uplad "+file.getFilename());
                Path fil = denneUploadMappe.resolve(tjekSikkerSti(file.getFilename()));
                Files.createDirectories(fil.getParent());
                Files.copy(file.getContent(), fil);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
*/