package eu.nitrogensensor.daisy;


import eu.nitrogensensor.daisylib.DaisyInvoker;
import eu.nitrogensensor.daisylib.DaisyModel;

import java.io.IOException;
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

    DaisyModel d = new DaisyModel("daisy/src/test/resources/Taastrup 2019/dtu_model", "Setup_DTU_Taastrup.dai");
    //System.out.println(d.getStarttime());
    //dry_bulk_density = d.Input['defhorizon'][0]['dry_bulk_density'].getvalue()
    //d.Input['defhorizon'][0]['dry_bulk_density'].setvalue(1.1*dry_bulk_density)
    //d.save_as(r'C:\Program Files\Daisy 5.72\exercises\Exercise01_new.dai')
    //DaisyModel.path_to_daisy_executable =  r'C:\Program Files\Daisy 5.72\bin\Daisy.exe'
    //d.run();

    d.replace("(stop *)", "(stop 2015 8 20)"); // for hurtigere kørsel
    //d.erstat("(stop *)", "(stop 2019 8 31)"); // fuld kørsel
    d.replace("(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")");
    

    String[] programmer = {
            "High_N_Low_W",
            "Low_N_Low_W",
            "Low_N_High_W",
            "High_N_High_W",
    };

    //d.outputEkstrakt.add(new Koersel.OutputEkstrakt("crop-leaf-stem-AI.csv", "crop.csv (year, month, mday, LAI), crop_prod.csv (year, month, mday, Crop AI, Leaf AI, Stem AI)"));
    d.outputEkstrakt.add(new DaisyModel.OutputEkstrakt("crop-leaf-stem-AI.csv", "crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)"));
    //d.outputfilnavne = new String[]{"crop.csv", "crop_prod.csv" };
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
          //Path tmpMappe = Files.createTempDirectory("ns-daisy");
          Path tmpMappe = Paths.get("/tmp/p6/out_"+program); // Anden mappe i udviklingsøjemed
          DaisyModel kørsel = d.cloneToDirectory(tmpMappe);
          kørsel.replace("(run taastrup)", "(run Mark21 (column (\""+program+"\")))");
          kørsel.beskrivelse = program;


          kørsel.klargørTilMappe2(tmpMappe);

          DaisyInvoker daisyInvoke = new DaisyInvoker();
          daisyInvoke.invokeDaisy(tmpMappe, "Setup_DTU_Taastrup.dai");


          kørsel.læsOutput(tmpMappe);

          for (DaisyModel.OutputEkstrakt ekstrakt1 : kørsel.outputEkstrakt) {
              ekstrakt1.lavUdtræk(kørsel.output);
          }

          for (DaisyModel.OutputEkstrakt ekstrakt : kørsel.outputEkstrakt) {
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
