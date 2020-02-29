package eu.nitrogensensor.daisylib.remote;

import com.google.gson.Gson;
import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.Utils;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.ObjectMapper;
import kong.unirest.Unirest;

import java.io.*;
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
        if (Server.url==null) Server.start(); url = Server.url; // for at starte og bruge lokal server
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

    private static String __oploadZip(Path inputDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.zipMappe(inputDir.toString(), baos);
        HttpResponse<String> oploadRes = Unirest.post(url+"/uploadZip")
                .field("zipfil",  new ByteArrayInputStream(baos.toByteArray()), "zipfil.zip")
                .asString();

        if (!oploadRes.isSuccess()) throw new IOException("Fik ikke oploaded filer: "+oploadRes.getBody());
        String batchId = oploadRes.getBody();
        return  batchId;
    }


    private static void __skriv(DaisyModel kørsel, ExtractedContent extractedContent, Path resultsDir) throws IOException {
        if (resultsDir != null) {
            Path resultDir = resultsDir.resolve(kørsel.getId());
            Utils.sletMappe(resultDir);
            Files.createDirectories(resultDir);
            if (FEJLFINDING) System.out.println("Skriver " + extractedContent.fileContensMap.keySet() + " til " + resultDir);
            for (String filnavn : extractedContent.fileContensMap.keySet()) {
                String filIndhold = extractedContent.fileContensMap.get(filnavn);
                Files.write(resultDir.resolve(filnavn), filIndhold.getBytes());
            }
        }
    }


    public static ArrayList<ExtractedContent> runParralel(ArrayList<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
        final ArrayList<ExtractedContent> extractedContents = new ArrayList<>();
        Path inputDir = getDirectory(daisyModels);
        String oploadId = __oploadZip(inputDir);

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
                try {
                    if (fejl.get() != null) return;
                    kørslerIgang.put(kørsel.getId(), "0 starter");

                    // Fjern irrelecante oplysninger fra det objekt, der sendes over netværket
                    ExecutionBatch batch = new ExecutionBatch();
                    batch.resultExtractor = resultExtractor;
                    batch.oploadId = oploadId;
                    batch.kørsel = kørsel.clon();
                    batch.kørsel.directory = null;
                    batch.kørsel.setId(null);

                    HttpResponse<ExtractedContent> response = Unirest.post(url + "/sim/").body(batch).asObject(ExtractedContent.class);
                    if (!response.isSuccess()) throw new IOException(response.getStatusText());

                    kørslerIgang.put(kørsel.getId(), "4 modtag");
                    if (!response.isSuccess()) {
                        System.err.println("Kald for "+ kørsel.getId()+" fejlede: "+response.getHeaders());
                        System.err.println("Kald fejlede1: "+response.getStatus() +response.getStatusText());
                        System.err.println("Kald fejlede2: "+response);
                        throw new IOException(response.getStatusText());
                    }

                    ExtractedContent extractedContent1 = response.getBody();
                    extractedContent1.id = kørsel.getId();
                    kørslerIgang.put(kørsel.getId(), "5 skriv");
                    __skriv(kørsel, extractedContent1, resultsDir);
                    ExtractedContent extractedContent = extractedContent1;
                    extractedContents.add(extractedContent);
                } catch (IOException e) {
                    System.err.println("FEJL i "+kørselsNr_+" "+kørsel.getId());
                    e.printStackTrace();
                    if (fejl.get() != null) return;
                    fejl.set(e);
                } finally {
                    kørslerIgang.remove(kørsel.getId());
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

    public static ArrayList<ExtractedContent> runSerial(ArrayList<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
        Path inputDir = getDirectory(daisyModels);
        String oploadId = __oploadZip(inputDir);
        ArrayList<ExtractedContent> extractedContents = new ArrayList<>();
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;

            // Fjern irrelecante oplysninger fra det objekt, der sendes over netværket
            ExecutionBatch batch = new ExecutionBatch();
            batch.resultExtractor = resultExtractor;
            batch.oploadId = oploadId;
            batch.kørsel = kørsel.clon();
            batch.kørsel.directory = null;
            batch.kørsel.setId(null);

            HttpResponse<ExtractedContent> response = Unirest.post(url + "/sim/").body(batch).asObject(ExtractedContent.class);
            if (!response.isSuccess()) throw new IOException(response.getStatusText());

            ExtractedContent extractedContent = response.getBody();
            extractedContent.id = kørsel.getId();
            extractedContents.add(extractedContent);
            __skriv(kørsel, extractedContent, resultsDir);
        }
        return extractedContents;
    }

}
