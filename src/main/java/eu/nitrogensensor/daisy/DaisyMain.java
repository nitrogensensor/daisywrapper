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
            "(run Mark21 (column (\"High_N_Low_W\")))",
//            "(run Mark21 (column (\"Low_N_Low_W\")))",
//            "(run Mark21 (column (\"Low_N_High_W\")))",
//            "(run Mark21 (column (\"High_N_High_W\")))",
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
          kørsel.erstat("(run taastrup)", program);
          kørsel.beskrivelse = program;
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
            String skilletegn = ", ";
            BufferedWriter bufferedWriter = Files.newBufferedWriter(kørsel.orgMappe.resolve(ekstrakt.output.filnavn));
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

  static Koersel.Ouputfilindhold getOuputfilindhold(Path tmpMappe, String filnavn) throws IOException {
    Koersel.Ouputfilindhold output = new Koersel.Ouputfilindhold();
    output.filnavn = filnavn;
    String csv = new String(Files.readAllBytes(tmpMappe.resolve(filnavn)));
    String[] csvsplit = csv.split("--------------------");
    output.header = csvsplit[0].trim();
    String[] linjer = csvsplit[1].trim().split("\n");
    if (output.kolonnenavne.size()!=0) throw new IllegalStateException("Outputfil er allerede parset");
    output.kolonnenavne.addAll(Arrays.asList(linjer[0].split("\t")));
    output.enheder.addAll(Arrays.asList(linjer[1].split("\t")));

    if (output.kolonnenavne.size() < output.enheder.size()) { // crop.csv har 24 kolonner, men 21 enheder (de sidste 3 kolonner er uden enhed), derfor < og ikke !=
      throw new IOException(filnavn + " har " +output.kolonnenavne.size() +" kolonner, men "+output.enheder.size()+
              " enheder\nkol="+ output.kolonnenavne +"\nenh="+output.enheder);
    }
    output.data = new ArrayList<>(linjer.length);
    for (int n=2; n<linjer.length; n++) {
      String[] linje = linjer[n].split("\t");
      if (output.kolonnenavne.size() < linje.length || linje.length < output.enheder.size()) { // data altid mellem
        throw new IOException(filnavn + " linje " + n +  " har " +linje.length +" kolonner, men "+
                output.kolonnenavne.size() + " kolonnenavne og "+
                output.enheder.size() +" enheder\nlin="+ Arrays.toString(linje) +" enheder\nkol="+ output.kolonnenavne +"\nenh="+output.enheder);
      }
      output.data.add(linje);
    }
    // Fyld op med tomme enheder
    while (output.enheder.size()<output.kolonnenavne.size()) output.enheder.add("");
    return output;
  }
}
