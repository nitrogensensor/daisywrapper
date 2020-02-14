package eu.nitrogensensor.executionservice;

import com.google.gson.Gson;
import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.ObjectMapper;
import kong.unirest.Unirest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class DaisyModelRemoteExecution {

    public static ArrayList<ExtractedContent> runSerial(ArrayList<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path result) throws IOException {

        Unirest.config().setObjectMapper(new ObjectMapper() {
            private Gson gson = MyGsonPathConverter.buildGson();

            @Override
            public <T> T readValue(String value, Class<T> valueType) {
                return gson.fromJson(value, valueType);
            }

            @Override
            public String writeValue(Object value) {
                return gson.toJson(value);
            }
        });

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

        ArrayList<ExtractedContent> extractedContents = new ArrayList<>();
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;

            // Fjern irrelecante oplysninger fra det objekt der sendes over netværket
            batch.kørsel = kørsel.clon();
            batch.kørsel.directory = null;
            batch.kørsel.setId(null);

            HttpResponse<ExtractedContent> response = Unirest.post(Server.url + "/sim/")
                    .body(batch)
                    .asObject(ExtractedContent.class);
            if (!response.isSuccess()) throw new IOException(response.getStatusText());
            ExtractedContent extractedContent = response.getBody();

            extractedContent.id = kørsel.getId();

            extractedContents.add(extractedContent);
        }
        Server.stop();

        return extractedContents;
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
