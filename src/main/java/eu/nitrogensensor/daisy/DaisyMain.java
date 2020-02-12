package eu.nitrogensensor.daisy;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DaisyMain
{

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("hej fra DaisyMain");

    DaisyInvoker daisyInvoke = new DaisyInvoker();

    String scriptFil = "Setup_DTU_Taastrup.dai";
    Path orgMappe = Paths.get("daisy/src/test/resources/Taastrup 2019/dtu_model");
    Koersel kørsel0 = new Koersel(orgMappe, scriptFil);
    kørsel0.erstat("stop 2018 8 20", "stop 2015 8 20"); // for hurtigere kørsel
    //kørsel0.erstat("stop 2018 8 20", "stop 2019 8 31"); // fuld kørsel
    kørsel0.erstat("(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")");

    String[] programmer = {
            "High_N_Low_W",
            "Low_N_Low_W",
            "Low_N_High_W",
            "High_N_High_W",
    };

    //kørsel0.outputEkstrakt.add(new Koersel.OutputEkstrakt("crop-leaf-stem-AI.csv", "crop.csv (year, month, mday, LAI), crop_prod.csv (year, month, mday, Crop AI, Leaf AI, Stem AI)"));
    kørsel0.outputEkstrakt.add(new Koersel.OutputEkstrakt("crop-leaf-stem-AI.csv", "crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)"));
    //kørsel0.outputfilnavne = new String[]{"crop.csv", "crop_prod.csv" };
    long tid = System.currentTimeMillis();

    ExecutorService executorService = Executors.newWorkStealingPool();
    AtomicReference<IOException> fejl = new AtomicReference<>(); // Hvis der opstår en exception skal den kastes videre
    int kørselsNr = 0;
    for (String program : programmer) {
      kørselsNr++;
      final int kørselsNr_ = kørselsNr;
      Runnable runnable = () -> {
        try {
          if (fejl.get() != null) return;
          Koersel kørsel = kørsel0.kopi();
          kørsel.erstat("(run taastrup)", "(run Mark21 (column (\""+program+"\")))");
          kørsel.beskrivelse = program;
          Path tmpMappe = Files.createTempDirectory("ns-daisy");
          tmpMappe = Paths.get("/tmp/p6/out_"+program); // Anden mappe i udviklingsøjemed


          kørsel.klargørTilMappe(tmpMappe);
          daisyInvoke.invokeDaisy(tmpMappe, scriptFil);


          kørsel.læsOutput(tmpMappe);

          for (Koersel.OutputEkstrakt ekstrakt1 : kørsel.outputEkstrakt) {
              ekstrakt1.lavUdtræk(kørsel.output);
          }

          for (Koersel.OutputEkstrakt ekstrakt : kørsel.outputEkstrakt) {
            // Skriv outputfil med ekstrakt
            Path fil = tmpMappe.resolve(ekstrakt.output.filnavn);
            String skilletegn = ", ";
            String header = "# Udtræk af "+ekstrakt.filKolonnerMap+" fra "+kørsel.scriptFil + "\n" +
                    "# "+kørsel.beskrivelse + "\n";
            ekstrakt.output.skrivDatafil(fil, skilletegn, header);

          }


          //System.out.println(kørsel);

        } catch (IOException e) {
          e.printStackTrace();
          fejl.set(e);
        } catch (Exception e) {
          e.printStackTrace();
          fejl.set(new IOException(e));
        }
      };
      //runnable.run(); // serielt
      executorService.submit(runnable); // parallelt
    }
    executorService.shutdown();
    executorService.awaitTermination(30, TimeUnit.MINUTES);
    if (fejl.get()!=null) throw fejl.get();

    System.out.printf("Det tog %.1f sek\n", (System.currentTimeMillis()-tid)/1000.0);
  }
}
