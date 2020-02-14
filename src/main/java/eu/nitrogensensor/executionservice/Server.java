package eu.nitrogensensor.executionservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.json.JavalinJson;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server {
    public static Javalin app;
    public static String url = "http://localhost:8080";

    public static void main(String[] args) {
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

        Gson gson = new GsonBuilder().create();
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
        app.post("/sim", ctx -> sim(ctx));
        app.post("/upload", ctx -> {
            Path upload = Paths.get("upload");
            Path uploadTmp = Files.createTempDirectory(upload,"");
            ctx.uploadedFiles("files").forEach(file -> {
                try {
                    System.out.println("Server uplad "+file.getFilename());
                    Path fil = uploadTmp.resolve(file.getFilename());
                    Files.createDirectories(fil.getParent());
                    Files.copy(file.getContent(), fil);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            String batchId = upload.relativize(uploadTmp).toString();
            ctx.html(batchId );
        });
    }

    private static void sim(Context ctx) {
        try {
            System.out.println("Server sim "+ctx.url());
            System.out.println("Server sim "+ctx.body());
            ExecutionBatch batch = JavalinJson.fromJson(ctx.body(),ExecutionBatch.class);
            //ExecutionBatch batch = ctx.bodyAsClass(ExecutionBatch.class);

            System.out.println("Server ExecutionBatch "+batch);
            ctx.json(batch);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
