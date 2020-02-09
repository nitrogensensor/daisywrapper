package eu.nitrogensensor.daisy;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DaisyMain
{
  private static final boolean FILPRÆFIX_PÅ_KOLONNER = true;

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("hej fra DaisyMain");

    DaisyInvoker daisyInvoke = new DaisyInvoker();

    String scriptFil = "Setup_DTU_Taastrup.dai";
    Path orgMappe = Paths.get("daisy/src/test/resources/Taastrup 2019/dtu_model");
    Koersel kørsel0 = new Koersel(orgMappe, scriptFil);
    //kørsel0.erstat("stop 2018 8 20", "stop 2015 8 20"); // for hurtigere kørsel
    kørsel0.erstat("(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")");

/*
    kørsel.erstat("(defprogram taastrup batch*)", "(defprogram taastrup batch (run (Mark21_hnlw)))");
    kørsel.erstat("(defprogram taastrup batch*)", "(defprogram taastrup batch (run (Mark21_lnlw)))");
    kørsel.erstat("(defprogram taastrup batch*)", "(defprogram taastrup batch (run (Mark21_lnhw)))");
    kørsel.erstat("(defprogram taastrup batch*)", "(defprogram taastrup batch (run (Mark21_hnhw)))");

    String[] programmer = {
            "(run (Mark21_hnlw))",
            "(run (Mark21_lnlw))",
            "(run (Mark21_lnhw))",
            "(run (Mark21_hnhw))",
    };
*/
    String[] programmer = {
            "(run Mark21 (column (\"High_N_Low_W\")))",
//            "(run Mark21 (column (\"Low_N_Low_W\")))",
//            "(run Mark21 (column (\"Low_N_High_W\")))",
//            "(run Mark21 (column (\"High_N_High_W\")))",
    };

    kørsel0.outputEkstrakt.add(new Koersel.OutputEkstrakt("crop-leaf-stem-AI.csv", "crop.csv (year, month, mday, LAI), crop_prod.csv (year, month, mday, Crop AI, Leaf AI, Stem AI)"));
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
          kørsel.erstat("(run taastrup)", program);
          Path tmpMappe = Files.createTempDirectory("ns-daisy");
          tmpMappe = Paths.get("/tmp/p5/k"+kørselsNr_); // Anden mappe i udviklingsøjemed
          //kørsel.klargørTilMappe(tmpMappe);
          //daisyInvoke.invokeDaisy(tmpMappe, scriptFil);


          HashSet<String> outputfilnavne = new HashSet<>();
          for (Koersel.OutputEkstrakt outputEkstrakt : kørsel.outputEkstrakt) {
            outputfilnavne.addAll(outputEkstrakt.filKolonnerMap.keySet());
          }
          for (String filnavn : outputfilnavne) {
            Koersel.Ouputfilindhold output = getOuputfilindhold(tmpMappe, filnavn);
            kørsel.output.put(filnavn, output);
          }

          for (Koersel.OutputEkstrakt outputEkstrakt : kørsel.outputEkstrakt) {

            ArrayList<String> kolonner = new ArrayList<>();
            ArrayList<String> enheder = new ArrayList<>();

            for (String filnavn : outputEkstrakt.filKolonnerMap.keySet()) {
              Koersel.Ouputfilindhold output = kørsel.output.get(filnavn);
              for (String kol : outputEkstrakt.filKolonnerMap.get(filnavn)) {
                if (FILPRÆFIX_PÅ_KOLONNER) kolonner.add(filnavn+":"+kol);
                else kolonner.add(kol);

                int idx = 0;
                while (idx<output.kolonnenavne.length && output.kolonnenavne[idx].equals(kol)) idx++;
                if (idx==output.kolonnenavne.length) throw new IllegalArgumentException("Kolonne '"+kol+"' fandtes ikke i "+output);


                enheder.add( output.enheder[idx] );
              }


            }


          }


          System.out.println(kørsel);

        } catch (IOException e) {
          e.printStackTrace();
          fejl.set(e);
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

  static Koersel.Ouputfilindhold getOuputfilindhold(Path tmpMappe, String filnavn) throws IOException {
    Koersel.Ouputfilindhold output = new Koersel.Ouputfilindhold();
    output.filnavn = filnavn;
    String csv = new String(Files.readAllBytes(tmpMappe.resolve(filnavn)));
    String[] csvsplit = csv.split("--------------------");
    output.header = csvsplit[0].trim();
    String[] linjer = csvsplit[1].trim().split("\n");
    output.kolonnenavne = linjer[0].split("\t");
    output.enheder = linjer[1].split("\t");

    if (output.kolonnenavne.length < output.enheder.length) { // crop.csv har 24 kolonner, men 21 enheder (de sidste 3 kolonner er uden enhed), derfor < og ikke !=
      throw new IOException(filnavn + " har " +output.kolonnenavne.length +" kolonner, men "+output.enheder.length+
              " enheder\nkol="+ Arrays.toString(output.kolonnenavne) +"\nenh="+Arrays.toString(output.enheder));
    }
    output.data = new ArrayList<>(linjer.length);
    for (int n=2; n<linjer.length; n++) {
      String[] linje = linjer[n].split("\t");
      if (output.kolonnenavne.length < linje.length || linje.length < output.enheder.length) { // data altid mellem
        throw new IOException(filnavn + " linje " + n +  " har " +linje.length +" kolonner, men "+
                output.kolonnenavne.length + " kolonnenavne og "+
                output.enheder.length +" enheder\nlin="+ Arrays.toString(linje) +" enheder\nkol="+ Arrays.toString(output.kolonnenavne) +"\nenh="+Arrays.toString(output.enheder));
      }
      output.data.add(linje);
    }
    // Fyld op med tomme enheder
    return output;
  }

  public void invokeDaisy(String mappe, String inputFil) throws InterruptedException, IOException {

  }
}
