package eu.nitrogensensor.daisylib;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class DaisyExecution {
    private static ConcurrentHashMap<String, String> kørslerIgang = new ConcurrentHashMap<String,String>();

    private static String visStatus() {
        HashMap<String, Integer> keyCountMap = new HashMap<String, Integer>();
        for(String v : new ArrayList<>(kørslerIgang.values()))
        {
            Integer i = keyCountMap.get(v);
            keyCountMap.put(v, i==null? 1 : i+1);
        }

        return String.format("%tT Der er %2d kørsler i gang: "+keyCountMap,new Date(), kørslerIgang.size());
    }


    public static void runSerial(Collection<DaisyModel> daisyModels) throws IOException {
        runSerial(daisyModels, null, null);
    }

    public static void runSerial(Collection<DaisyModel> daisyModels, ResultExtractor re, Path resultsDir) throws IOException {
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;
            if (re != null) re.tjekResultatIkkeAlleredeFindes(kørsel.directory);
            kørsel.run();
            if (re != null) re.extractToDirectory(kørsel.directory, resultsDir.resolve(kørsel.getId()));
        }
    }

    public static void runParralel(Collection<DaisyModel> daisyModels) throws IOException {
        runParralel(daisyModels, null, null);
    }

    public static void runParralel(Collection<DaisyModel> daisyModels, Path resultsDir) throws IOException {
        runParralel(daisyModels, new ResultExtractor(), resultsDir);
    }


    public static void runParralel(Collection<DaisyModel> daisyModels, ResultExtractor re, Path resultsDir) throws IOException {
        AtomicReference<IOException> fejl = new AtomicReference<>(); // Hvis der opstår en exception skal den kastes videre
        ForkJoinPool executorService = (ForkJoinPool) Executors.newWorkStealingPool(); // LOKAL variabel - kan ikke genbruges da vi kalder shutdown() længere nede
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;
            if (re != null) re.tjekResultatIkkeAlleredeFindes(kørsel.directory);
            if (Utils.debug) System.out.println(visStatus() + " kørsel "+kørselsNr+" af "+daisyModels.size()+ " startes.");
            final int kørselsNr_ = kørselsNr;
            Runnable runnable = () -> {
                try {
                    kørslerIgang.put(kørsel.getId(), "0 starter");
                    if (fejl.get() != null) return;
                    kørslerIgang.put(kørsel.getId(), "3 kører");
                    kørsel.run();
                    kørslerIgang.put(kørsel.getId(), "5 skriver");
                    if (re != null) re.extractToDirectory(kørsel.directory, resultsDir.resolve(kørsel.getId()));
                } catch (IOException e) {
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
            if (Utils.debug) System.out.println(visStatus()+ " og "+executorService.getQueuedSubmissionCount()+" er i kø.");
            try { Thread.sleep(1000); } catch (Exception e) { };
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        if (fejl.get()!=null) throw fejl.get();
    }
}
