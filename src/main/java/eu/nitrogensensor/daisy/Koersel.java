package eu.nitrogensensor.daisy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Koersel implements Cloneable {
    public final Path orgMappe;
    public final String scriptFil;
    public String beskrivelse;
    private ArrayList<Erstatning> erstatninger = new ArrayList<>();


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

    public void læsOutput(Path tmpMappe) throws IOException {
        HashSet<String> outputfilnavne = new HashSet<>();
        for (Koersel.OutputEkstrakt outputEkstrakt : outputEkstrakt) {
            outputfilnavne.addAll(outputEkstrakt.filKolonnerMap.keySet());
        }
        for (String filnavn : outputfilnavne) {
            Koersel.Ouputfilindhold ouputfilindhold = getOuputfilindhold(tmpMappe, filnavn);
            output.put(filnavn, ouputfilindhold);
        }
    }

    private static final boolean FILPRÆFIX_PÅ_KOLONNER = false;
    public void lavUdtræk() {
        for (OutputEkstrakt ekstrakt : outputEkstrakt) {
            // Opbyg liste over kolonner og enheder
            int antalRækker = -1;
            for (String filnavn : ekstrakt.filKolonnerMap.keySet()) {
                Ouputfilindhold outputfil = output.get(filnavn);
                for (String kol : ekstrakt.filKolonnerMap.get(filnavn)) {
                    if (FILPRÆFIX_PÅ_KOLONNER)
                        ekstrakt.output.kolonnenavne.add(filnavn + ":" + kol);
                    else
                        ekstrakt.output.kolonnenavne.add(kol);

                    int idx = outputfil.kolonnenavne.indexOf(kol);
                    if (ekstrakt.filKolonneIndexMap.get(filnavn) == null)
                        ekstrakt.filKolonneIndexMap.put(filnavn, new ArrayList<>());
                    ekstrakt.filKolonneIndexMap.get(filnavn).add(idx);
                    if (idx == -1) throw new IllegalArgumentException("Kolonne '" + kol + "' fandtes ikke i " + outputfil);
                    ekstrakt.output.enheder.add(outputfil.enheder.get(idx));

                    if (antalRækker != -1 && antalRækker != outputfil.data.size())
                        throw new IllegalStateException("Forventede " + antalRækker + " datarækker i " + outputfil);
                    antalRækker = outputfil.data.size();
                }
            }

            // Lav datarækket
            for (int række = 0; række < antalRækker; række++) {
                String[] datalineE = new String[ekstrakt.output.kolonnenavne.size()];
                int kolE = 0;
                for (String filnavn : ekstrakt.filKolonnerMap.keySet()) {
                    Ouputfilindhold outputfil = output.get(filnavn);
                    for (int kol1 : ekstrakt.filKolonneIndexMap.get(filnavn)) {
                        // Tag højde for at nogle af de sidste kolonner i en Daisy CSV fil kan være tomme
                        datalineE[kolE] = outputfil.data.get(række).length <= kol1 ? "" : outputfil.data.get(række)[kol1];
                        kolE++;
                    }
                }
                ekstrakt.output.data.add(datalineE);
            }
        }
    }

    public static class Ouputfilindhold {
        public String filnavn;
        public String header;
        public ArrayList<String> kolonnenavne = new ArrayList<>();
        public ArrayList<String> enheder = new ArrayList<>();
        public ArrayList<String[]> data = new ArrayList<>();

        @Override
        public String toString() {
            return "Ouputfilindhold{" +
                    "'" + filnavn + '\'' +
                    ", " + kolonnenavne.size() +" kolonner"+
                    ", " + (data==null?0:data.size())+" rækker" +
                    '}';
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

        public void skrivDatafil(Path fil, String skilletegn, String header) throws IOException {
            Files.deleteIfExists(fil);
            BufferedWriter bufferedWriter = Files.newBufferedWriter(fil);
            bufferedWriter.append(header);
            printRække(skilletegn, kolonnenavne, bufferedWriter);
            printRække(skilletegn, enheder, bufferedWriter);
            for (String[] datarække : data) {
                printRække(skilletegn, datarække, bufferedWriter);
            }
            bufferedWriter.close();
        }
    }

    public Map<String, Ouputfilindhold> output = new LinkedHashMap<String, Ouputfilindhold>();

    public static void main(String[] args) {
        new OutputEkstrakt("xx", "crop.csv (year, month, mday, LAI), crop_prod.csv (year, month, mday, Crop AI, Leaf AI, Stem AI)");
        new OutputEkstrakt("xx", "crop.csv (*)");
        new OutputEkstrakt("xx", "crop.csv");
    }

    public ArrayList<OutputEkstrakt> outputEkstrakt = new ArrayList<>();
    public static class OutputEkstrakt {
        public LinkedHashMap<String, ArrayList<String>> filKolonnerMap = new LinkedHashMap<String, ArrayList<String>>();
        public final LinkedHashMap<String, ArrayList<Integer>> filKolonneIndexMap = new LinkedHashMap<String, ArrayList<Integer>>();
        public final Ouputfilindhold output = new Ouputfilindhold();

        public OutputEkstrakt(OutputEkstrakt org) {
            filKolonnerMap = (LinkedHashMap<String, ArrayList<String>>) org.filKolonnerMap.clone();
            output.filnavn = org.output.filnavn;
        }

        @Override
        public String toString() {
            return "OutoutEkstrakt{" +
                    ", filKolonnerMap=" + filKolonnerMap +
                    '}';
        }

        /**
         *
         * @param skrivTilFilnavn Hvilken fil ekstraktet skal skrives til
         * @param indhold Hvilket indhold fra hvilke filer der skal trækkes ud. F.eks. giver "crop.csv (year    month   mday LAI), crop_prod.csv (year    month   mday, Crop AI Leaf AI Stem AI)"
         *                kolonnerne (year    month   mday LAI) fra crop.csv og (year    month   mday, Crop AI Leaf AI Stem AI) fra crop_prod.csv.
         *                "crop.csv (*)" eller blot "crop.csv" giver hele indholdet af en fil
         */
        public OutputEkstrakt(String skrivTilFilnavn, String indhold) {
            output.filnavn = skrivTilFilnavn;

            Matcher filnavnMatcher = Pattern.compile("[a-zA-Z_. ]+(?![^(]*\\))").matcher(indhold);


            String forrigeFil = null;
            int forrigeMatchSlutpos = 0;
            while (filnavnMatcher.find()) {
                if (forrigeFil!=null) {
                    String forrigeKol = indhold.substring(forrigeMatchSlutpos, filnavnMatcher.start());
                    filKolonnerMap.put(forrigeFil, findKolonner(forrigeKol));
                }

                forrigeFil = filnavnMatcher.group().trim();
                forrigeMatchSlutpos = filnavnMatcher.end();
            }
            if (forrigeFil!=null) {
                String forrigeKol = indhold.substring(forrigeMatchSlutpos);
                filKolonnerMap.put(forrigeFil, findKolonner(forrigeKol));
            }

            System.out.println(indhold);
            System.out.println(this);
        }

        private ArrayList<String> findKolonner(String sb) {
            String trimTegn = "(*\t\n ),";
            // Trim parenteser etc i enderne væk
            //System.out.println(sb);
            int start=0;
            int slut=sb.length()-1;
            while (start<=slut && trimTegn.indexOf(sb.charAt(start))!=-1) start++;
            while (start<=slut && trimTegn.indexOf(sb.charAt(slut))!=-1) slut--;

            String s = sb.substring(start, slut+1);
            //System.out.println(s);
            ArrayList<String> kolonner = new ArrayList<>();
            for (String kol : s.split("[,\t]+")) kolonner.add(kol.trim());
            return kolonner;
        }
    }



    public Koersel(Path orgMappe, String scriptFil) {
        this.orgMappe = orgMappe;
        this.scriptFil = scriptFil;
    }

    /** Opretter en kopi af en kørsel og kopi af dets erstatninger */
    public Koersel kopi() {
        if (!output.isEmpty()) throw new IllegalStateException("Du bør ikke bruge en kørsel der allerede har output som skabelon for en anden kørsel");
        try {
            Koersel kopi = (Koersel) this.clone();
            kopi.erstatninger = new ArrayList<>();
            kopi.erstatninger.addAll(this.erstatninger);
            kopi.outputEkstrakt = new ArrayList<OutputEkstrakt>();
            for (OutputEkstrakt ekstrakt : outputEkstrakt) {
                kopi.outputEkstrakt.add(new OutputEkstrakt(ekstrakt));
            }
            kopi.output = new LinkedHashMap<String, Ouputfilindhold>();
            return kopi;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Koersel{" +
                "orgMappe=" + orgMappe +
                ", scriptFil='" + scriptFil + '\'' +
                ", erstatninger=" + erstatninger +
                ", outputfilnavne=" + outputEkstrakt +
                ", output=" + output +
                '}';
    }

    public void klargørTilMappe(Path destMappe) throws IOException {
        Utils.klonMappe(orgMappe, destMappe);

        String scriptIndholdOrg = new String(Files.readAllBytes(orgMappe.resolve(scriptFil)));

        String scriptIndhold = Erstatning.udfør(scriptIndholdOrg, erstatninger);

        // Overskriv scriptfil med den, hvor diverse felter er blevet erstattet
        Path scriptfilITmp = destMappe.resolve(scriptFil);
        Files.delete(scriptfilITmp);
        Files.write(scriptfilITmp, scriptIndhold.getBytes());
    }

    public Koersel erstat(String søgestreng, String erstatning) {
        erstatninger.add(new Erstatning(søgestreng, erstatning));
        return this;
    }
}
