package eu.nitrogensensor.daisylib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutputEkstrakt {
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

    // Trækker en bestemt fil ud
    public OutputEkstrakt(String filnavn) {
        this(filnavn, filnavn);
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

    private static final boolean FILPRÆFIX_PÅ_KOLONNER = false;

    public void lavUdtræk(Map<String, Ouputfilindhold> output) {
        OutputEkstrakt ekstrakt = this;
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

    public static void main(String[] args) {
        new OutputEkstrakt("xx", "crop.csv (year, month, mday, LAI), crop_prod.csv (year, month, mday, Crop AI, Leaf AI, Stem AI)");
        new OutputEkstrakt("xx", "crop.csv (*)");
        new OutputEkstrakt("xx", "crop.csv");
        new OutputEkstrakt("crop.csv");
    }
}
