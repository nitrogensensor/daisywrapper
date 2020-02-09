package eu.nitrogensensor.daisy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Koersel implements Cloneable {
    public final Path orgMappe;
    public final String scriptFil;
    private ArrayList<Erstatning> erstatninger = new ArrayList<>();

    public static class Ouputfilindhold {
        public String filnavn;
        public String header;
        public String[] kolonnenavne;
        public String[] enheder;
        public ArrayList<String[]> data;

        @Override
        public String toString() {
            return "Ouputfilindhold{" +
                    "'" + filnavn + '\'' +
                    ", " + kolonnenavne.length +" kolonner"+
                    ", " + (data==null?0:data.size())+" rækker" +
                    '}';
        }
    }
    public final Map<String, Ouputfilindhold> output = new LinkedHashMap<String, Ouputfilindhold>();

    public static void main(String[] args) {
        new OutputEkstrakt("xx", "crop.csv (year, month, mday, LAI), crop_prod.csv (year, month, mday, Crop AI, Leaf AI, Stem AI)");
        new OutputEkstrakt("xx", "crop.csv (*)");
        new OutputEkstrakt("xx", "crop.csv");
    }

    public ArrayList<OutputEkstrakt> outputEkstrakt = new ArrayList<>();
    public static class OutputEkstrakt {
        public final LinkedHashMap<String, ArrayList<String>> filKolonnerMap = new LinkedHashMap<String, ArrayList<String>>();
        public final Ouputfilindhold output = new Ouputfilindhold();

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


        @Override
        public String toString() {
            return "OutoutEkstrakt{" +
                    ", filKolonnerMap=" + filKolonnerMap +
                    '}';
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
