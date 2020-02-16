package eu.nitrogensensor.executionservice;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.json.JavalinJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Server {
    public static Javalin app;
    public static String url = "http://localhost:8080";

    //System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
    static { System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT %4$s %2$s %5$s%6$s%n"); }
    static Logger log = Logger.getGlobal();


    public static void main(String[] args) {
        log.info("hej1");
        start();
        Klient.testkald();
        stop();
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
            System.out.println("Server "+ctx.method()+" på " +ctx.url());
        });
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
        });
        app.get("/", ctx -> ctx.result("Hello World"));
        app.get("/json", ctx -> ctx.result("Hello World"));
        app.post("/upload", ctx -> upload(ctx));
        app.post("/sim", ctx -> sim(ctx));
    }

    private static Path upload = Paths.get("upload");
    private static void upload(Context ctx) throws IOException {
            Path directory = Files.createTempDirectory(upload,"");
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
            String batchId = upload.relativize(directory).toString();
            ctx.html(batchId);
    }

    private static void sim(Context ctx) throws IOException {
            System.out.println("Server sim "+ctx.url());
            System.out.println("Server sim "+ctx.body());
            ExecutionBatch batch = JavalinJson.fromJson(ctx.body(),ExecutionBatch.class);
            //ExecutionBatch batch = ctx.bodyAsClass(ExecutionBatch.class);
            batch.kørsel.directory =  upload.resolve(tjekSikkerSti(batch.oploadId));

            System.out.println("Server ExecutionBatch "+batch.oploadId);
            batch.kørsel.run();
            ExtractedContent extractedContent = new ExtractedContent();
            batch.resultExtractor.extract(batch.kørsel.directory, extractedContent.fileContensMap);
            ctx.json(extractedContent);
    }

    private static String tjekSikkerSti(String sti0) {
        String sti = sti0;
        if (sti.startsWith("/")) sti = sti.substring(1); // fjern / i starten
        sti.replace("..", "");
        if (!sti.equals(sti0)) new IllegalArgumentException("Usikker sti "+sti0+" lavet om til "+sti).printStackTrace();
        return sti;
    }
}