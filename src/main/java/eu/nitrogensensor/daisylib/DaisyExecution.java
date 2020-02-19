package eu.nitrogensensor.daisylib;

import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.csv.CsvEkstraktor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DaisyExecution {

    public static void runSerial(ArrayList<DaisyModel> daisyModels) throws IOException {
        runSerial(daisyModels, null, null);
    }

    public static void runSerial(ArrayList<DaisyModel> daisyModels, ResultExtractor re, Path resultsDir) throws IOException {
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;
            kørsel.run();
            if (re != null) re.extract(kørsel.directory, resultsDir.resolve(kørsel.getId()));
        }
    }

    public static void runParralel(ArrayList<DaisyModel> daisyModels) throws IOException {
        runParralel(daisyModels, null, null);
    }

    public static void runParralel(ArrayList<DaisyModel> daisyModels, ResultExtractor re, Path resultsDir) throws IOException {
        ExecutorService executorService = Executors.newWorkStealingPool();
        AtomicReference<IOException> fejl = new AtomicReference<>(); // Hvis der opstår en exception skal den kastes videre
        int kørselsNr = 0;
        for (DaisyModel kørsel : daisyModels) {
            kørselsNr++;
            final int kørselsNr_ = kørselsNr;
            Runnable runnable = () -> {
                if (fejl.get() != null) return;
                try {
                    kørsel.run();
                    if (re != null) re.extract(kørsel.directory, resultsDir.resolve(kørsel.getId()));
                } catch (IOException e) {
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
    }
}
