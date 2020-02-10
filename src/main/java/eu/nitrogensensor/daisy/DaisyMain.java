package eu.nitrogensensor.daisy;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DaisyMain
{
  private static final boolean FILPRÆFIX_PÅ_KOLONNER = false;

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

          for (Koersel.OutputEkstrakt ekstrakt : kørsel.outputEkstrakt) {
            // Opbyg liste over kolonner og enheder
            int antalRækker = -1;
            for (String filnavn : ekstrakt.filKolonnerMap.keySet()) {
              Koersel.Ouputfilindhold outputfil = kørsel.output.get(filnavn);
              for (String kol : ekstrakt.filKolonnerMap.get(filnavn)) {
                if (FILPRÆFIX_PÅ_KOLONNER)
                  ekstrakt.output.kolonnenavne.add(filnavn+":"+kol);
                else
                  ekstrakt.output.kolonnenavne.add(kol);

                int idx = outputfil.kolonnenavne.indexOf(kol);
                if (ekstrakt.filKolonneIndexMap.get(filnavn)==null) ekstrakt.filKolonneIndexMap.put(filnavn, new ArrayList<>());
                ekstrakt.filKolonneIndexMap.get(filnavn).add(idx);
                if (idx==-1) throw new IllegalArgumentException("Kolonne '"+kol+"' fandtes ikke i "+outputfil);
                ekstrakt.output.enheder.add( outputfil.enheder.get(idx));

                if (antalRækker!=-1 && antalRækker!=outputfil.data.size()) throw new IllegalStateException("Forventede "+ antalRækker+ " datarækker i "+outputfil);
                antalRækker=outputfil.data.size();
              }
            }

            // Lav datarækket
            for (int række=0; række<antalRækker; række++) {
              String[] datalineE = new String[ekstrakt.output.kolonnenavne.size()];
              int kolE = 0;
              for (String filnavn : ekstrakt.filKolonnerMap.keySet()) {
                Koersel.Ouputfilindhold outputfil = kørsel.output.get(filnavn);
                for (int kol1 : ekstrakt.filKolonneIndexMap.get(filnavn)) {
                  // Tag højde for at nogle af de sidste kolonner i en Daisy CSV fil kan være tomme
                  datalineE[kolE] = outputfil.data.get(række).length<=kol1 ? "" : outputfil.data.get(række)[kol1];
                  kolE++;
                }
              }
              ekstrakt.output.data.add(datalineE);
            }

            // Skriv outputfil med ekstrakt
            Path fil = tmpMappe.resolve(ekstrakt.output.filnavn);
            String skilletegn = ", ";
            Files.deleteIfExists(fil);
            BufferedWriter bufferedWriter = Files.newBufferedWriter(fil);
            bufferedWriter.append("# Udtræk af "+ekstrakt.filKolonnerMap+" fra "+kørsel.scriptFil).append('\n');
            bufferedWriter.append("# "+kørsel.beskrivelse).append('\n');
            printRække(skilletegn, ekstrakt.output.kolonnenavne, bufferedWriter);
            printRække(skilletegn, ekstrakt.output.enheder, bufferedWriter);
            for (String[] datarække : ekstrakt.output.data) {
              printRække(skilletegn, datarække, bufferedWriter);
            }
            bufferedWriter.close();

          }


          System.out.println(kørsel);

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


    System.out.printf("Det tog %.1f sek", (System.currentTimeMillis()-tid)/1000.0);
  }

  static void printRække(String skilletegn, ArrayList<String> række, BufferedWriter bufferedWriter) throws IOException {
    boolean førsteKolonne = true;
    for (String k : række) {
      if (!førsteKolonne) bufferedWriter.append(skilletegn);
      bufferedWriter.append(k);
      førsteKolonne = false;
    }
    bufferedWriter.append('\n');
  }

  static void printRække(String skilletegn, String[] række, BufferedWriter bufferedWriter) throws IOException {
    boolean førsteKolonne = true;
    for (String k : række) {
      if (!førsteKolonne) bufferedWriter.append(skilletegn);
      bufferedWriter.append(k);
      førsteKolonne = false;
    }
    bufferedWriter.append('\n');
  }
}
