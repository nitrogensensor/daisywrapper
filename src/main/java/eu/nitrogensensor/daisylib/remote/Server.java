package eu.nitrogensensor.daisylib.remote;

import com.google.gson.Gson;
import eu.nitrogensensor.daisylib.Utils;
import eu.nitrogensensor.daisylib.remote.google_cloud_storage.GemOgHentArbejdsfiler;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.javalin.plugin.json.JavalinJson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Server {
    private static final boolean USIKKER_KØR = false;
    public static boolean PAK_UD_VED_MODTAGELSEN = true;
    public static Javalin app;
    public static String url;

    //System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
    static { System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT %4$s %2$s %5$s%6$s%n"); }
    static Logger log = Logger.getGlobal();
    static { log.setLevel( Level.ALL ); log.getParent().getHandlers()[0].setLevel(Level.ALL); } // vis alt log


    public static void main(String[] args) {
        log.info("hej1 med log.info");
        log.fine("hej1 med log.fine");
        log.warning("hej1 med log.warning");
        start();
        //Testklient.testkald();
        //stop();
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
        app.get("/", ctx -> ctx.contentType("text/html").result("<html><body>Du kan også spørge på <a href='json'>json</a>"));
        app.get("/json", ctx -> ctx.result("Hello World"));
        app.post("/uploadZip", ctx -> upload(ctx));
        if (USIKKER_KØR) app.get("/koer", ctx -> kør(ctx, null));
        app.post("/sim", ctx -> sim(ctx));
    }

    private static String kør(Context ctx, String[] parms) throws IOException, InterruptedException {
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

    private static String upload(Context ctx) throws IOException {
        Files.createDirectories(uploadMappe);

        Path denneUploadMappe = Files.createTempDirectory(uploadMappe,"");
        String batchId = uploadMappe.relativize(denneUploadMappe).toString();
        System.out.println("upload får batchId = " + batchId);

        UploadedFile file = ctx.uploadedFile("zipfil");
        if (file!=null) {
            System.out.println("Server upladZip " + file.getFilename());
            InputStream is = file.getContent();
            if (PAK_UD_VED_MODTAGELSEN) {
                if (!is.markSupported()) throw new RuntimeException("is skal kunne spoles tilbage");
                is.mark(Integer.MAX_VALUE);
                Utils.unzipMappe(is, denneUploadMappe.toString());
                is.reset();
            }
            GemOgHentArbejdsfiler.gem(is, batchId);
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

    private static void sim(Context ctx) throws IOException {
            System.out.println("Server sim "+ctx.url());
            System.out.println("Server sim "+ctx.body());
            ExecutionBatch batch = JavalinJson.fromJson(ctx.body(),ExecutionBatch.class);
            if (batch==null) log.fine("Ingen batch fra "+ctx.body());
            else {
                //ExecutionBatch batch = ctx.bodyAsClass(ExecutionBatch.class);
                batch.kørsel.directory = uploadMappe.resolve(tjekSikkerSti(batch.oploadId));
                if (Files.exists(batch.kørsel.directory) && batch.kørsel.directory.toFile().list().length>0) {
                    log.fine("Denne instans har allerede batch "+batch.kørsel.directory+" med "
                            + Files.list(batch.kørsel.directory).collect(Collectors.toList()));
                } else {
                    log.fine("Denne instans har ikke batch "+batch.kørsel.directory);

                    Path fil = GemOgHentArbejdsfiler.hent(batch.oploadId);
                    InputStream fis = Files.newInputStream(fil);
                    Utils.unzipMappe(fis, batch.kørsel.directory.toString());
                    fis.close();
                    Files.delete(fil);
                }
                System.out.println("Server ExecutionBatch " + batch.oploadId);

                // Vi skal have det over i en anden midlertidig mappe, ellers kan det være vi får knas med at
                // evt andre samtidige kørsler skriver i de samme outputfiler
                batch.kørsel.copyToDirectory(Files.createTempDirectory("kørsel_"+batch.kørsel.getId()));
                batch.resultExtractor.tjekResultatIkkeAlleredeFindes(batch.kørsel.directory); // burde egentlig ikke være nødvendigt, da det også tjekkes af klienten, men man kan ikke stole på klienter...
                batch.kørsel.run();
                ExtractedContent extractedContent = new ExtractedContent();
                batch.resultExtractor.extract(batch.kørsel.directory, extractedContent.fileContensMap);
                ctx.json(extractedContent);
                Utils.sletMappe(batch.kørsel.directory); // ryd op
            }
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