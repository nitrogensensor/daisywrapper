package eu.nitrogensensor.daisylib.remote;

import com.google.gson.Gson;
import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.Utils;
import kong.unirest.HttpResponse;
import kong.unirest.ObjectMapper;
import kong.unirest.RequestBodyEntity;
import kong.unirest.Unirest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class DaisyRemoteExecution {
    private static final boolean FEJLFINDING = false;

    public static int MAX_PARALLELITET = 500;
    public static final int MAX_KØRSELSTID = 1000*60*60; // 1 time

    private static final ConcurrentHashMap<String, String> kørslerIgang = new ConcurrentHashMap<>();
    public static int maxSamtidigeKørslerIgang;

    private static Gson gson = MyGsonPathConverter.buildGson();
    //private static String remoteEndpointUrl = "https://daisykoersel-6dl4uoo23q-lz.a.run.app";
    private static String remoteEndpointUrl = "http://nitrogen.saluton.dk:3210";

    public static void setRemoteEndpointUrl(String url) {
        while (url.endsWith("/")) url = url.substring(0, url.length()-1); // fjern afsluttende /-streger
        remoteEndpointUrl = url;
    }

    static {
        //if (Server.url==null) Server.start(); url = Server.url; MAX_PARALLELITET=8; // for at starte og bruge lokal server
        String remoteEndpointUrl1 = System.getenv("DAISY_REMOTE_URL");
        if (remoteEndpointUrl1!=null) remoteEndpointUrl = remoteEndpointUrl1;

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

    private static Path getDirectory(Collection<DaisyModel> daisyModels) {
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

        return String.format("%tT Der %2d kørsler i gang: %s", new Date(), kørslerIgang.size(), keyCountMap.toString());
    }

    private static String __oploadZip(Path inputDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.zipMappe(inputDir.toString(), baos);
        String url = remoteEndpointUrl +"/uploadZip";
        System.out.println("Oploader ZIP-fil af "+inputDir+" på "+baos.size()/1000.0 + " kb til "+url);
        HttpResponse<String> oploadRes = Unirest.post(url)
                .field("zipfil",  new ByteArrayInputStream(baos.toByteArray()), "zipfil.zip")
                .asString();

        if (!oploadRes.isSuccess()) throw new IOException("Fik ikke oploaded filer: "+oploadRes.getBody()+" for URL "+oploadRes);
        String batchId = oploadRes.getBody();
        if (Utils.debug) System.out.println("Fik oploadet "+inputDir+".zip og fik batch ID "+batchId);
        return  batchId;
    }


    public static void writeExtractedContentToSubdir(ExtractedContent extractedContent, Path resultsDir) throws IOException {
        if (Utils.debug) System.out.println("extractedContent.id er "+extractedContent.id);
        Path resultDir = resultsDir.resolve(extractedContent.id.replaceAll("[^A-Za-z0-9_]", "_"));
        Utils.sletMappe(resultDir);
        Files.createDirectories(resultDir);
        //if (FEJLFINDING)
        if (Utils.debug) System.out.println("Skriver " + extractedContent.fileContensMap.keySet() + " til " + resultDir);
        for (String filnavn : extractedContent.fileContensMap.keySet()) {
            String filIndhold = extractedContent.fileContensMap.get(filnavn);
            Path fil = resultDir.resolve(filnavn);
            if (Utils.debug) System.out.println("opretter "+fil.toString()+ " i "+fil.getParent());// kald ikke, kan give exception: +" "+Files.readAttributes(fil.getParent(), "*"));
            Files.createDirectories(fil.getParent());
            Files.write(fil, filIndhold.getBytes());
        }
    }

    public static Map<String, ExtractedContent> runParralel(Collection<DaisyModel> daisyModels) throws IOException {
        return runParralel(daisyModels, null, null);
    }

    public static Map<String, ExtractedContent> runParralel(Collection<DaisyModel> daisyModels, ResultExtractor resultExtractor) throws IOException {
        return runParralel(daisyModels, resultExtractor, null);
    }

    public static Map<String, ExtractedContent> runParralel(Collection<DaisyModel> daisyModels, Path resultsDir) throws IOException {
        return runParralel(daisyModels, null, resultsDir);
    }

    public static Map<String, ExtractedContent> runParralel(Collection<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException {

        final Map<String, ExtractedContent> extractedContents = new ConcurrentHashMap<>();
        Path inputDir = getDirectory(daisyModels);
        if (resultExtractor!=null) resultExtractor.tjekResultatIkkeAlleredeFindes(inputDir);

        String oploadId = __oploadZip(inputDir); // HER OPLOADES!!!!!

        //ExecutorService executorService = Executors.newWorkStealingPool();
        int antalKørsler = daisyModels.size();
        int parallelitet = Math.max(5, Math.min(MAX_PARALLELITET, antalKørsler/4)); // minimum 4 kørsler per instans - men mindst 5 instanser
        if (Utils.debug) System.out.println("parallelitet: "+parallelitet);

        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(daisyModels.size(),parallelitet)); // max 100 parallele forespørgsler
        AtomicReference<Throwable> fejl = new AtomicReference<>(); // Hvis der opstår en exception skal den kastes videre
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;
            // Klodset og sikkkert nytteløst forsøg på at håndtere at båndbredden til endpointet sikkert er begrænset
            if (kørslerIgang.size()>100) try { Thread.sleep(50); } catch (Exception e) {}
            if (Utils.debug) System.out.println(visStatus() + ". Kørsel "+kørselsNr+" af "+daisyModels.size()+ " sættes i kø.");
            kørslerIgang.put(kørsel.getId(), "afventer");

            final int kørselsNr_ = kørselsNr;
            Runnable runnable = () -> {
                try {
                    if (fejl.get() != null) return;
                    kørslerIgang.put(kørsel.getId(), "sender");
                    System.out.println(visStatus() + ". Kørsel "+kørselsNr_+" "+kørsel.getId()+" sendes nu.");

                    // Fjern irrelecante oplysninger fra det objekt, der sendes over netværket
                    ExecutionBatch batch = new ExecutionBatch();
                    batch.resultExtractor = resultExtractor;
                    batch.oploadId = oploadId;
                    batch.kørsel = kørsel.createCopy();
                    batch.kørsel.setId(kørsel.getId()); // til sporing på serversiden
                    batch.kørsel.directory = null;

                    RequestBodyEntity response0 = Unirest.post(remoteEndpointUrl + "/sim/").body(batch);
                    if (fejl.get() != null) return;
                    kørslerIgang.put(kørsel.getId(), "modtager");
                    HttpResponse<ExtractedContent> response = response0.asObject(ExtractedContent.class);
                    //System.out.println(visStatus() + " kørsel "+kørselsNr_+" 4 modtag.");
                    if (!response.isSuccess()) {
                        System.err.println("Serverfejl for "+ kørselsNr_+" "+ kørsel.getId()+": "+response.getStatus() + " " +response.getStatusText());
                        System.err.println("Serverfejl body: "+response0.asString().getBody());
                        fejl.set(new IOException(response.getStatusText()));
                        return;
                    }

                    ExtractedContent extractedContent = response.getBody();
                    extractedContent.id = kørsel.getId();
                    extractedContents.put(kørsel.getId(), extractedContent);
                    if (resultsDir != null) writeExtractedContentToSubdir(extractedContent, resultsDir);

                    if (extractedContent.exception != null) {
                        extractedContent.exception.printStackTrace(); // vis fejlen
                        fejl.set(extractedContent.exception);
                    }
                } catch (Throwable e) {
                    System.err.println("Klientside fejl i "+kørselsNr_+" "+kørsel.getId());
                    e.printStackTrace();
                    if (fejl.get() != null) return;
                    fejl.set(e);
                } finally {
                    kørslerIgang.remove(kørsel.getId());
                    synchronized (kørslerIgang) { kørslerIgang.notifyAll(); }
                }
            };
            executorService.submit(runnable); // parallelt
            //runnable.run(); // serielt
        }
        while (kørslerIgang.size()>0) {
            if (Utils.debug) System.out.println(visStatus());
            synchronized (kørslerIgang) { try { kørslerIgang.wait(5000); } catch (Exception e) { }};
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(MAX_KØRSELSTID, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        if (fejl.get()!=null) throw new IOException(fejl.get());
        return extractedContents;
    }

    public static ArrayList<ExtractedContent> runSerial(Collection<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException {
        Path inputDir = getDirectory(daisyModels);
        resultExtractor.tjekResultatIkkeAlleredeFindes(inputDir);
        String oploadId = __oploadZip(inputDir);
        ArrayList<ExtractedContent> extractedContents = new ArrayList<>();
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;

            // Fjern irrelecante oplysninger fra det objekt, der sendes over netværket
            ExecutionBatch batch = new ExecutionBatch();
            batch.resultExtractor = resultExtractor;
            batch.oploadId = oploadId;
            batch.kørsel = kørsel.createCopy();
            batch.kørsel.directory = null;

            HttpResponse<ExtractedContent> response = Unirest.post(remoteEndpointUrl + "/sim/").body(batch).asObject(ExtractedContent.class);
            if (!response.isSuccess()) throw new IOException(response.getStatusText());

            ExtractedContent extractedContent = response.getBody();
            extractedContent.id = kørsel.getId();
            extractedContents.add(extractedContent);
            if (resultsDir!=null) writeExtractedContentToSubdir(extractedContent, resultsDir);
        }
        return extractedContents;
    }

    public static String getKørselstype() {
        if (maxSamtidigeKørslerIgang==0) return "Lokalt";
        if (remoteEndpointUrl.contains("localhost")) return "Lokal server";
        if (remoteEndpointUrl.contains("run.app")) return "Cloud Run";

        System.out.println("Ukendt kørselsetype for "+ remoteEndpointUrl);
        return remoteEndpointUrl;
    }
}
