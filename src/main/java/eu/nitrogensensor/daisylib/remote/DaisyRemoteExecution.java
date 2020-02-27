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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DaisyRemoteExecution {
    private static final boolean FEJLFINDING = false;

    public static final int MAX_PARALLELITET = 400;
    public static final int MAX_KØRSELSTID = 1000*60*60; // 1 time

    private static ConcurrentHashMap<String, String> oploadsIgang = new ConcurrentHashMap<String,String>();
    private static ConcurrentHashMap<String, String> kørslerIgang = new ConcurrentHashMap<String,String>();
    public static int maxSamtidigeKørslerIgang;

    private static Gson gson = MyGsonPathConverter.buildGson();
    private static String url = "https://daisykoersel-6dl4uoo23q-lz.a.run.app";
    static {
        Server.start(); url = Server.url; // for at starte og bruge lokal server
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
        Unirest.config().concurrency(MAX_PARALLELITET, MAX_PARALLELITET);
        Unirest.config().socketTimeout(MAX_KØRSELSTID);
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
        if (FEJLFINDING) System.out.println("filer="+filer);
        for (Path fil : filer) {
            oploadReq = oploadReq.field("files", fil.toFile(), inputDir.relativize(fil).toString());
        }
        return oploadReq;
    }


    public static ArrayList<ExtractedContent> runSerial0(ArrayList<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
        Path inputDir = getDirectory(daisyModels);

        ArrayList<Path> filer = new ArrayList<Path>();
        Files.walk(inputDir).filter(fraFil -> !Files.isDirectory(fraFil)).forEach(f -> filer.add(f));
        if (FEJLFINDING) System.out.println("filer="+filer);

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
                if (FEJLFINDING) System.out.println("Skriver "+extractedContent.fileContensMap.keySet()+" til " +resultDir);
                for (String filnavn : extractedContent.fileContensMap.keySet()) {
                    String filIndhold = extractedContent.fileContensMap.get(filnavn);
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
        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(daisyModels.size(),MAX_PARALLELITET)); // max 100 parrallel forespørgsler
        AtomicReference<IOException> fejl = new AtomicReference<>(); // Hvis der opstår en exception skal den kastes videre
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;
            // Klodset og sikkkert nytteløst forsøg på at håndtere at båndbredden til opload fra en enkelt maskine er begrænset
            if (kørselsNr>20) try { Thread.sleep(50); } catch (Exception e) {}
            if (oploadsIgang.size()>20) try { Thread.sleep(100); } catch (Exception e) {}
            if (oploadsIgang.size()>40) try { Thread.sleep(200); } catch (Exception e) {}
            if (oploadsIgang.size()>60) try { Thread.sleep(4000); } catch (Exception e) {}
            System.out.println(visStatus() + " kørsel "+kørselsNr+" af "+daisyModels.size()+ " startes.");

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
        while (kørslerIgang.size()>0) {
            System.out.println(visStatus());
            try { Thread.sleep(1000); } catch (Exception e) { };
        }


        executorService.shutdown();
        try {
            executorService.awaitTermination(MAX_KØRSELSTID, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        if (fejl.get()!=null) throw fejl.get();
        return extractedContents;
    }

    private static String visStatus() {
        HashMap<String, Integer> keyCountMap = new HashMap<String, Integer>();
        for(String v : new ArrayList<>(kørslerIgang.values()))
        {
            Integer i = keyCountMap.get(v);
            keyCountMap.put(v, i==null? 1 : i+1);
        }
        maxSamtidigeKørslerIgang = Math.max(maxSamtidigeKørslerIgang, kørslerIgang.size());

        return String.format("%tT Der er %2d oploads og %2d kørsler i gang: "+keyCountMap,new Date(), oploadsIgang.size(), kørslerIgang.size());
    }

    public static ExtractedContent uploadSim(DaisyModel kørsel, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
        kørslerIgang.put(kørsel.getId(), "0 starter");
        MultipartBody oploadReq = Unirest.post(url + "/uploadsim").multiPartContent();
        kørslerIgang.put(kørsel.getId(), "1 tilf filer");
        oploadReq = tilføjInputfilerTilRequest(oploadReq, kørsel.directory);

        kørslerIgang.put(kørsel.getId(), "2 batch");
        ExecutionBatch batch = new ExecutionBatch();
        batch.resultExtractor = resultExtractor;
        // Fjern irrelevante oplysninger fra det objekt, der sendes over netværket
        batch.kørsel = kørsel.clon();
        batch.kørsel.directory = null;
        batch.kørsel.setId(null);
        oploadReq.field("batch", gson.toJson(batch));
        kørslerIgang.put(kørsel.getId(), "3 opload/sim");
        oploadReq.uploadMonitor((field, fileName, bytesWritten, totalBytes) -> {
            String nøgle = kørsel.getId()+" "+field+" "+fileName;
            if (bytesWritten < totalBytes) {
                boolean nyt = oploadsIgang.put(nøgle, nøgle)==null;
                if (FEJLFINDING) if (nyt) System.out.println(kørsel.getId() + " " + field + " " + fileName + " " + bytesWritten + "/" + totalBytes);
            } else {
                oploadsIgang.remove(nøgle);
                if (FEJLFINDING) System.out.println(kørsel.getId() + " " + field + " " + fileName + " færdig " + oploadsIgang.size()+" oploads i gang");
            }
        });
        HttpResponse<ExtractedContent> response = oploadReq.asObject(ExtractedContent.class);
        kørslerIgang.put(kørsel.getId(), "4 modtag");
        if (!response.isSuccess()) {
            System.err.println("Kald for "+kørsel.getId()+" fejlede: "+response.getHeaders());
            System.err.println("Kald fejlede1: "+response.getStatus() +response.getStatusText());
            System.err.println("Kald fejlede2: "+response);
            throw new IOException(response.getStatusText());
        }

        ExtractedContent extractedContent = response.getBody();
        extractedContent.id = kørsel.getId();
        kørslerIgang.put(kørsel.getId(), "5 skriv");
        if (resultsDir!=null) {
            Path resultDir = resultsDir.resolve(kørsel.getId());
            Utils.sletMappe(resultDir);
            Files.createDirectories(resultDir);
            if (FEJLFINDING) System.out.println("Skriver "+extractedContent.fileContensMap.keySet()+" i "+resultDir);
            for (String filnavn : extractedContent.fileContensMap.keySet()) {
                String filIndhold = extractedContent.fileContensMap.get(filnavn);
                Files.write(resultDir.resolve(filnavn), filIndhold.getBytes());
            }
        }
        kørslerIgang.remove(kørsel.getId());
        return extractedContent;
    }
}
