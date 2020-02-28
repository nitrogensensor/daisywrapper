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
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final boolean USIKKER_KØR = false;
    public static Javalin app;
    public static String url = "http://localhost:8080";

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
        if (app!=null) return;

        Gson gson = MyGsonPathConverter.buildGson();
        JavalinJson.setFromJsonMapper(gson::fromJson);
        JavalinJson.setToJsonMapper(gson::toJson);

        String port = System.getenv("PORT");
        if (port == null) {
            port = "8080";
        }

        app = Javalin.create().start(Integer.parseInt(port));
        app.before(ctx -> {
            // runs before all requests
            log.fine("Server "+ctx.method()+" på " +ctx.url());
        });
        app.exception(Exception.class, (e, ctx) -> {
            log.warning(e.toString());
            e.printStackTrace();
        });
        app.get("/", ctx -> ctx.contentType("text/html").result("<html><body>Du kan også spørge på <a href='json'>json</a>"));
        app.get("/json", ctx -> ctx.result("Hello World"));
        app.post("/upload", ctx -> upload(ctx));
        app.post("/uploadZip", ctx -> uploadZip(ctx));
        if (USIKKER_KØR) app.get("/koer", ctx -> kør(ctx, null));
        app.post("/sim", ctx -> sim(ctx));
        app.post("/uploadsim", ctx -> uploadsim(ctx));
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
        Path directory = Files.createTempDirectory(uploadMappe,"");
            ctx.uploadedFiles("files").forEach(file -> {
                try {
                    System.out.println("Server uplad "+file.getFilename());
                    Path fil = directory.resolve(tjekSikkerSti(file.getFilename()));
                    Files.createDirectories(fil.getParent());
                    Files.copy(file.getContent(), fil);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        String batchId = uploadMappe.relativize(directory).toString();
        ctx.html(batchId);
        return batchId;
    }

    private static String uploadZip(Context ctx) throws IOException {
        Files.createDirectories(uploadMappe);
        Path dataMappe = Files.createTempDirectory(uploadMappe,"");
        UploadedFile file = ctx.uploadedFile("data");
        System.out.println("Server upladZip "+file.getFilename());
        InputStream is = file.getContent();
        if (!is.markSupported()) throw new RuntimeException("is skal kunne spoles tilbage");
        is.mark(Integer.MAX_VALUE);
        Utils.unzipMappe(is, "/tmp/xxx");
        is.reset();
        Utils.unzipMappe(is, "/tmp/xxx1");
        is.reset();
        Utils.unzipMappe(is, "/tmp/xxx2");
        is.reset();
        String batchId = uploadMappe.relativize(dataMappe).toString();
        GemOgHentArbejdsfiler.gem(file.getContent(), batchId);
        ctx.html(batchId);
        return batchId;
    }

    private static void sim(Context ctx) throws IOException {
            System.out.println("Server sim "+ctx.url());
            System.out.println("Server sim "+ctx.body());
            ExecutionBatch batch = JavalinJson.fromJson(ctx.body(),ExecutionBatch.class);
            if (batch==null) log.fine("Ingen batch fra "+ctx.body());
            else {
                //ExecutionBatch batch = ctx.bodyAsClass(ExecutionBatch.class);
                batch.kørsel.directory = uploadMappe.resolve(tjekSikkerSti(batch.oploadId));

                System.out.println("Server ExecutionBatch " + batch.oploadId);
                batch.kørsel.run();
                ExtractedContent extractedContent = new ExtractedContent();
                batch.resultExtractor.extract(batch.kørsel.directory, extractedContent.fileContensMap);
                ctx.json(extractedContent);
            }
    }


    private static void uploadsim(Context ctx) throws IOException {
        ExecutionBatch batch = JavalinJson.fromJson(ctx.formParam("batch"),ExecutionBatch.class);
        batch.oploadId = upload(ctx);
        //ExecutionBatch batch = ctx.bodyAsClass(ExecutionBatch.class);
        batch.kørsel.directory =  uploadMappe.resolve(tjekSikkerSti(batch.oploadId));

        System.out.println("Server ExecutionBatch "+batch.oploadId);
        batch.kørsel.run();
        ExtractedContent extractedContent = new ExtractedContent();
        batch.resultExtractor.extract(batch.kørsel.directory, extractedContent.fileContensMap);
        ctx.json(extractedContent);
        Utils.sletMappe(batch.kørsel.directory);  // ingenting caches for nu - ny opload hver gang :-|
    }


    private static String tjekSikkerSti(String sti0) {
        String sti = sti0;
        if (sti.startsWith("/")) sti = sti.substring(1); // fjern / i starten
        sti.replace("..", "");
        if (!sti.equals(sti0)) new IllegalArgumentException("Usikker sti "+sti0+" lavet om til "+sti).printStackTrace();
        return sti;
    }
}
