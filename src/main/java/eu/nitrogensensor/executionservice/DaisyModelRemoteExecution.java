package eu.nitrogensensor.executionservice;

import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DaisyModelRemoteExecution {

    public static void runSerial(ArrayList<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path result) throws IOException {

        Path dir = getDirectory(daisyModels);
        Server.start();


        ArrayList<Path> filer = new ArrayList<Path>();
        Files.walk(dir).filter(fraFil -> !Files.isDirectory(fraFil)).forEach(f -> filer.add(f));
        System.out.println("filer="+filer);

        MultipartBody oploadReq = Unirest.post(Server.url + "/upload").multiPartContent();
        for (Path fil : filer) oploadReq = oploadReq.field("files", Files.newInputStream(fil), dir.relativize(fil).toString());
        HttpResponse<String> oploadRes = oploadReq.asString();

        if (!oploadRes.isSuccess()) throw new IOException("Fik ikke oploaded filer: "+oploadRes.getBody());


        ExecutionBatch batch = new ExecutionBatch();
        batch.oploadId = oploadRes.getBody();
        batch.resultExtractor = resultExtractor;

        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;

           // batch.kørsel = kørsel;

            HttpResponse<String> response = Unirest.post(Server.url + "/sim/")
                    .body(batch)
                    .asString();
            //System.exit(0);

            if (!response.isSuccess()) throw new IOException(response.getBody());
            break;
        }

        System.exit(0);
        Server.stop();
    }


    private static Path getDirectory(ArrayList<DaisyModel> daisyModels) {
        Path directory = null;
        for (DaisyModel kørsel : daisyModels) {
            if (directory==null) directory = kørsel.directory;
            else if (!directory.equals(kørsel.directory)) throw new IllegalArgumentException("Some have different directories: "+directory + " "+kørsel.directory);
        }
        return directory;
    }
}
