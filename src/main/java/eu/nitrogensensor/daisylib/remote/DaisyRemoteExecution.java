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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DaisyRemoteExecution {
    private static Gson gson = MyGsonPathConverter.buildGson();
    private static String url = "https://daisykoersel-6dl4uoo23q-lz.a.run.app";

    static {
        // url = Server.url; // til lokal afprøvning
        Unirest.config().setObjectMapper(new ObjectMapper() {
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

    private static Path getDirectory(ArrayList<DaisyModel> daisyModels) {
        Path directory = null;
        for (DaisyModel kørsel : daisyModels) {
            if (directory==null) directory = kørsel.directory;
            else if (!directory.equals(kørsel.directory)) throw new IllegalArgumentException("Some have different directories: "+directory + " "+kørsel.directory);
        }
        return directory;
    }

    public static MultipartBody tilføjInputfilerTilRequest(MultipartBody oploadReq, Path inputDir) throws IOException {
        ArrayList<Path> filer = new ArrayList<Path>();
        Files.walk(inputDir).filter(fraFil -> !Files.isDirectory(fraFil)).forEach(f -> filer.add(f));
        System.out.println("filer="+filer);
        for (Path fil : filer) {
            oploadReq = oploadReq.field("files", fil.toFile(), inputDir.relativize(fil).toString());
        }
        return oploadReq;
    }


    public static ArrayList<ExtractedContent> runSerial0(ArrayList<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
        Path inputDir = getDirectory(daisyModels);

        ArrayList<Path> filer = new ArrayList<Path>();
        Files.walk(inputDir).filter(fraFil -> !Files.isDirectory(fraFil)).forEach(f -> filer.add(f));
        System.out.println("filer="+filer);

        MultipartBody oploadReq = Unirest.post(url + "/upload").multiPartContent();
        oploadReq = tilføjInputfilerTilRequest(oploadReq, inputDir);
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

            HttpResponse<ExtractedContent> response = Unirest.post(url + "/sim/")
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
        return extractedContents;
    }


    public static ArrayList<ExtractedContent> runSerial(ArrayList<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
        ArrayList<ExtractedContent> extractedContents = new ArrayList<>();
        for (DaisyModel kørsel : daisyModels) {
            ExtractedContent extractedContent = uploadSim(kørsel, resultExtractor, resultsDir);
            extractedContents.add(extractedContent);
        }
        return extractedContents;
    }


    public static ArrayList<ExtractedContent> runParralel(ArrayList<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
        final ArrayList<ExtractedContent> extractedContents = new ArrayList<>();

        //ExecutorService executorService = Executors.newWorkStealingPool();
        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(daisyModels.size(),200)); // max 100 parrallel forespørgsler
        AtomicReference<IOException> fejl = new AtomicReference<>(); // Hvis der opstår en exception skal den kastes videre
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;
            final int kørselsNr_ = kørselsNr;
            Runnable runnable = () -> {
                if (fejl.get() != null) return;
                try {
                    ExtractedContent extractedContent = uploadSim(kørsel, resultExtractor, resultsDir);
                    extractedContents.add(extractedContent);
                } catch (IOException e) {
                    System.err.println("FEJL i "+kørselsNr_+" "+kørsel.getId());
                    e.printStackTrace();
                    if (fejl.get() != null) return;
                    fejl.set(e);
                }
            };
            executorService.submit(runnable); // parallelt
            //runnable.run(); // serielt
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        if (fejl.get()!=null) throw fejl.get();
        return extractedContents;
    }

    public static ExtractedContent uploadSim(DaisyModel kørsel, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
        MultipartBody oploadReq = Unirest.post(url + "/uploadsim").multiPartContent();
        oploadReq = tilføjInputfilerTilRequest(oploadReq, kørsel.directory);

        ExecutionBatch batch = new ExecutionBatch();
        batch.resultExtractor = resultExtractor;
        // Fjern irrelecante oplysninger fra det objekt, der sendes over netværket
        batch.kørsel = kørsel.clon();
        batch.kørsel.directory = null;
        batch.kørsel.setId(null);

        oploadReq.field("batch", gson.toJson(batch));
        HttpResponse<ExtractedContent> response = oploadReq.asObject(ExtractedContent.class);
        if (!response.isSuccess()) {
            System.err.println("Kald for "+kørsel.getId()+" fejlede: "+response.getHeaders());
            System.err.println("Kald fejlede1: "+response.getStatus() +response.getStatusText());
            System.err.println("Kald fejlede2: "+response);
            throw new IOException(response.getStatusText());
        }

        ExtractedContent extractedContent = response.getBody();
        extractedContent.id = kørsel.getId();
        if (resultsDir!=null) {
            Path resultDir = resultsDir.resolve(kørsel.getId());
            Utils.sletMappe(resultDir);
            Files.createDirectories(resultDir);
            for (String filnavn : extractedContent.fileContensMap.keySet()) {
                String filIndhold = extractedContent.fileContensMap.get(filnavn);
                //System.out.println("Skriver til "+dir.resolve(filnavn));
                Files.write(resultDir.resolve(filnavn), filIndhold.getBytes());
            }
            System.out.println("Skrev "+extractedContent.fileContensMap.keySet()+" i "+resultDir);
        }
        return extractedContent;
    }
}
