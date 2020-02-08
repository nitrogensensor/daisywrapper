package eu.nitrogensensor.daisy;


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

    DaisyInvoker daisyInvoke = new DaisyInvoker();

    String scriptFil = "Setup_DTU_Taastrup.dai";
    Path orgMappe = Paths.get("daisy/src/test/resources/Taastrup 2019/dtu_model");
    Koersel kørsel0 = new Koersel(orgMappe, scriptFil);
    //kørsel0.erstat("stop 2018 8 20", "stop 2015 8 20"); // for hurtigere kørsel
    kørsel0.erstat("(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")");

    String[] programmer = {
            "(run (Mark21_hnlw))",
            "(run (Mark21_lnlw))",
            "(run (Mark21_lnhw))",
            "(run (Mark21_hnhw))",
    };
/*
    kørsel.erstat("(defprogram taastrup batch*)", "(defprogram taastrup batch (run (Mark21_hnlw)))");
    kørsel.erstat("(defprogram taastrup batch*)", "(defprogram taastrup batch (run (Mark21_lnlw)))");
    kørsel.erstat("(defprogram taastrup batch*)", "(defprogram taastrup batch (run (Mark21_lnhw)))");
    kørsel.erstat("(defprogram taastrup batch*)", "(defprogram taastrup batch (run (Mark21_hnhw)))");
*/

    long tid = System.currentTimeMillis();

    ExecutorService executorService = Executors.newWorkStealingPool();
    AtomicReference<IOException> fejl = new AtomicReference<>(); // Hvis der opstår en exception skal den kastes videre
    for (String program : programmer) {

      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          if (fejl.get()!=null) return;
          Koersel k = new Koersel(kørsel0).erstat("(run taastrup)", program);
          Path tmpMappe = Paths.get("/tmp/p/work_"+program); // Files.createTempDirectory("work");
          try {
            k.klargørTilMappe(tmpMappe);
            daisyInvoke.invokeDaisy(tmpMappe, scriptFil );//daisy/src/test/resources/Taastrup 2019/dtu_model", "Setup_DTU_Taastrup.dai");
          } catch (IOException e) {
            e.printStackTrace();
            fejl.set(e);
          }
        }
      };
      //runnable.run(); // serielt
      executorService.submit(runnable); // parallelt
    }
    executorService.shutdown();
    executorService.awaitTermination(30, TimeUnit.MINUTES);
    if (fejl.get()!=null) throw fejl.get();


    System.out.printf("Det tog %.1f sek", (System.currentTimeMillis()-tid)/1000.0);

  }

  public void invokeDaisy(String mappe, String inputFil) throws InterruptedException, IOException {

  }

}
