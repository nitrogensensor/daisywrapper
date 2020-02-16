package eu.nitrogensensor.daisylib.remote;

import com.google.gson.Gson;
import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.Utils;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.ObjectMapper;
import kong.unirest.Unirest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class DaisyRemoteExecution {

    static {
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
    }

    public static ArrayList<ExtractedContent> runSerial(ArrayList<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
//        Server.start();

        Path inputDir = getDirectory(daisyModels);


        ArrayList<Path> filer = new ArrayList<Path>();
        Files.walk(inputDir).filter(fraFil -> !Files.isDirectory(fraFil)).forEach(f -> filer.add(f));
        System.out.println("filer="+filer);

        MultipartBody oploadReq = Unirest.post(Server.url + "/upload").multiPartContent();
        for (Path fil : filer) oploadReq = oploadReq.field("files", Files.newInputStream(fil), inputDir.relativize(fil).toString());
        HttpResponse<String> oploadRes = oploadReq.asString();

        if (!oploadRes.isSuccess()) throw new IOException("Fik ikke oploaded filer: "+oploadRes.getBody());

        ExecutionBatch batch = new ExecutionBatch();
        batch.oploadId = oploadRes.getBody();
        batch.resultExtractor = resultExtractor;

        ArrayList<ExtractedContent> extractedContents = new ArrayList<>();
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;

            // Fjern irrelecante oplysninger fra det objekt, der sendes over netværket
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
            if (resultsDir!=null) {
                Path resultDir = resultsDir.resolve(kørsel.getId());
                Utils.sletMappe(resultDir);
                Files.createDirectories(resultDir);
                for (String filnavn : extractedContent.fileContensMap.keySet()) {
                    String filIndhold = extractedContent.fileContensMap.get(filnavn);
                    //System.out.println("Skriver til "+dir.resolve(filnavn));
                    Files.write(resultDir.resolve(filnavn), filIndhold.getBytes());
                }
            }
        }
//        Server.stop();

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
