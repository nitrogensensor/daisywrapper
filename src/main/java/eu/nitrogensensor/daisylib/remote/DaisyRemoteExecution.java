package eu.nitrogensensor.daisylib.remote;

import com.google.gson.Gson;
import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.Utils;
import kong.unirest.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DaisyRemoteExecution {
    public static final int MAX_KØRSELSTID = 1000*60*60; // 1 time

    private static final ConcurrentHashMap<String, String> kørslerIgang = new ConcurrentHashMap<>();
    public static int maxSamtidigeKørslerIgang;

    private static Gson gson = MyGsonPathConverter.buildGson();
    private static String remoteEndpointUrl = "http://daisy.nitrogensensor.eu:3210";

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
        //Unirest.config().concurrency(MAX_PARALLELITET, MAX_PARALLELITET);
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

        return String.format("%tT %2d runs pending %s - %s", new Date(), kørslerIgang.size(), keyCountMap.toString(), Utils.klipStreng(kørslerIgang, 200));
    }

    private static String __oploadZip(Path inputDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.zipMappe(inputDir.toString(), baos);
        String url = remoteEndpointUrl +"/uploadZip";
        System.out.println("Upload directory "+inputDir.toAbsolutePath()+" ("+baos.size()/1024.0 + " kb compressed) to remote server at "+url);
        HttpResponse<String> oploadRes = Unirest.post(url)
                .field("zipfil",  new ByteArrayInputStream(baos.toByteArray()), "zipfil.zip")
                .asString();

        if (!oploadRes.isSuccess()) throw new IOException("Fik ikke oploaded filer: "+oploadRes.getBody()+" for URL "+oploadRes);
        String batchId = oploadRes.getBody();
        if (Utils.debug) System.out.println("Fik oploadet "+inputDir+".zip og fik batch ID "+batchId);
        return  batchId;
    }


    public static void writeExtractedContentToSubdir(ExtractedContent extractedContent, Path resultsDir) throws IOException {
        Path resultDir = resultsDir.resolve(extractedContent.id.replaceAll("[^A-Za-z0-9_]", "_"));
        Utils.sletMappe(resultDir);
        Files.createDirectories(resultDir);
        System.out.println("Writing " + resultDir+"/");
        //if (Utils.debug) System.out.println("Skriver " +extractedContent.id+ " til " + resultDir  + ": " + extractedContent.fileContensMap.keySet());
        for (String filnavn : extractedContent.fileContensMap.keySet()) {
            String filIndhold = extractedContent.fileContensMap.get(filnavn);
            Path fil = resultDir.resolve(filnavn);
            //if (Utils.debug) System.out.println("Opretter "+fil.toString()); // kald ikke, kan give exception: +" "+Files.readAttributes(fil.getParent(), "*"));
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

    public static Map<String, ExtractedContent> runParralel(Collection<DaisyModel> daisyModels, ResultExtractor resultExtractor, Path resultsDir) throws IOException  {

        final Map<String, ExtractedContent> extractedContents = new ConcurrentHashMap<>();
        Path inputDir = getDirectory(daisyModels);
        if (resultExtractor!=null) resultExtractor.tjekResultatIkkeAlleredeFindes(inputDir);

        String oploadId = __oploadZip(inputDir);

        int parallelitet = daisyModels.size();
        if (getKørselstype().startsWith("Cloud")) {
            // I cloud Run skal vi have minimum 4 kørsler per instans - men dog mindst 5 instanser
            parallelitet = Math.max(Math.min(parallelitet, 5), parallelitet/4);
        }
        int MAX_PARALLELITET = remoteEndpointUrl.contains("run.app")? 500 : 15; // 500 for cloud run, 15 for nitrogen.saluton.dk
        parallelitet = Math.min(MAX_PARALLELITET, parallelitet); // max 100 parallele forespørgsler

        System.out.println("There are "+daisyModels.size()+" simulations. Will run up to "+parallelitet+" simulations in parrallel.");

        ExecutorService executorService = Executors.newFixedThreadPool(parallelitet);
        AtomicReference<Throwable> fejl = new AtomicReference<>(); // Hvis der opstår en exception skal den kastes videre
        AtomicInteger antalFejl = new AtomicInteger();
        int kørselsNr = 0;
        CountDownLatch countDownLatch = new CountDownLatch(daisyModels.size());
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;
            // Klodset og sikkkert nytteløst forsøg på at håndtere at båndbredden til endpointet sikkert er begrænset
            if (kørslerIgang.size()>100) try { Thread.sleep(50); } catch (Exception e) {}
            //if (Utils.debug) System.out.println(visStatus() + ". Kørsel "+kørselsNr+" af "+daisyModels.size()+ " sættes i kø.");
            kørslerIgang.put(kørsel.getId(), "not started");

            final int kørselsNr_ = kørselsNr;
            Runnable runnable = () -> {
                try {
                    if (fejl.get() != null) {
                        kørslerIgang.put(kørsel.getId(), "canceled");
                        return;
                    }

                    // Fjern irrelecante oplysninger fra det objekt, der sendes over netværket
                    ExecutionBatch batch = new ExecutionBatch();
                    batch.resultExtractor = resultExtractor;
                    batch.oploadId = oploadId;
                    batch.kørsel = kørsel.createCopy();
                    batch.kørsel.setId(kørsel.getId()); // til sporing på serversiden
                    batch.kørsel.directory = null;

                    kørslerIgang.put(kørsel.getId(), "running");
                    RequestBodyEntity response0 = Unirest.post(remoteEndpointUrl + "/sim/").body(batch);

                    //System.out.println("response0.asString().getBody() = " + response0.asString().getBody());
                    HttpResponse<ExtractedContent> response = response0.asObject(ExtractedContent.class);
                    if (Utils.debug) System.out.println("Kørsel "+kørsel.getId()+" modtog svar "+response.getStatus()+" "+response.getStatusText()+" isSuccess()="+response.isSuccess());

                    if (!response.isSuccess()) {
                        fejl.set(new IOException(response.getStatusText()));
                        System.err.println("(Server)fejl for "+ kørsel.getId()+": "+response.getStatus() + " " +response.getStatusText());
                        /*
                        System.out.flush();
                        System.err.flush();
                        Thread.sleep(100);
                        response.getParsingError().ifPresent(e -> {
                            if (antalFejl.getAndIncrement()>20) return;
                            e.printStackTrace();
                            System.err.println("Original body: " + e.getOriginalBody());
                        });
                        Files.write(Paths.get("/tmp/FejlFejl.bin"), response0.asBytes().getBody());
                        String body = response0.asString().getBody();
                        System.err.println("Serverfejl for: "+ kørsel.getId()+ "body: "+Utils.klipStreng(body, 500));
                        // System.exit(-1);
                         */
                        kørslerIgang.put(kørsel.getId(), "remote error");
                    }
                    kørslerIgang.put(kørsel.getId(), "extracting");

                    ExtractedContent extractedContent = response.getBody();
                    extractedContent.id = kørsel.getId();
                    extractedContents.put(kørsel.getId(), extractedContent);
                    if (resultExtractor.cleanCsvOutput) ResultExtractor.cleanCsv(extractedContent.fileContensMap);

                    if (resultsDir != null) writeExtractedContentToSubdir(extractedContent, resultsDir);

                    if (extractedContent.exception != null) {
                        extractedContent.exception.printStackTrace(); // vis fejlen
                        fejl.set(extractedContent.exception);
                    }
                    kørslerIgang.remove(kørsel.getId());
                } catch (Throwable e) {
                    kørslerIgang.put(kørsel.getId(), "extract error");
                    System.err.println("Klientside fejl i "+kørselsNr_+" "+kørsel.getId());
                    if (antalFejl.getAndIncrement()>20) return;
                    e.printStackTrace();
                    if (fejl.get()==null) fejl.set(e);
                } finally {
                    countDownLatch.countDown();
                }
            };
            executorService.submit(runnable); // parallelt
            //runnable.run(); // serielt
        }

        try {
            while (!countDownLatch.await(15, TimeUnit.SECONDS)) System.out.println(visStatus());
            System.out.println("DaisyRemoteExecution: Alt er afsluttet - med evt fejl="+fejl);
            if (fejl.get()!=null) throw new IOException(fejl.get());
            executorService.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
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
        if (remoteEndpointUrl.contains("localhost")) return "Lokal server";
        if (remoteEndpointUrl.contains("run.app")) return "Cloud Run";

        System.out.println("Ukendt kørselsetype for "+ remoteEndpointUrl);
        return remoteEndpointUrl;
    }
}
