package eu.nitrogensensor.daisylib.csv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvEkstraktor {
    public final String skrivTilFilnavn;
    public LinkedHashMap<String, ArrayList<String>> filKolonnerMap = new LinkedHashMap<String, ArrayList<String>>();

    @Override
    public String toString() {
        return "OutoutEkstrakt{" +
                ", filKolonnerMap=" + filKolonnerMap +
                '}';
    }

    /**
     *
     * @param indhold Hvilket indhold fra hvilke filer der skal trækkes ud. F.eks. giver "crop.csv (year    month   mday LAI), crop_prod.csv (year    month   mday, Crop AI Leaf AI Stem AI)"
     *                kolonnerne (year    month   mday LAI) fra crop.csv og (year    month   mday, Crop AI Leaf AI Stem AI) fra crop_prod.csv.
     *                "crop.csv (*)" eller blot "crop.csv" giver hele indholdet af en fil
     * @param skrivTilFilnavn Hvilken fil ekstraktet skal skrives til
     */
    public CsvEkstraktor(String indhold, String skrivTilFilnavn) {
        this.skrivTilFilnavn = skrivTilFilnavn;

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

        //System.out.println(indhold);
        //System.out.println(this);
    }

    // Trækker en bestemt fil ud
    public CsvEkstraktor(String filnavn) {
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

    public CsvFile lavUdtræk(Map<String, CsvFile> outputMap) {
        CsvEkstraktor ekstrakt = this;
        CsvFile output = new CsvFile();
        output.filnavn = skrivTilFilnavn;

        // Opbyg liste over kolonner og enheder
        LinkedHashMap<String, ArrayList<Integer>> filKolonneIndexMap = new LinkedHashMap<String, ArrayList<Integer>>();
        int antalRækker = -1;
        for (String filnavn : ekstrakt.filKolonnerMap.keySet()) {
            CsvFile outputfil = outputMap.get(filnavn);
            for (String kol : ekstrakt.filKolonnerMap.get(filnavn)) {
                output.kolonnenavne.add(kol);

                int idx = outputfil.kolonnenavne.indexOf(kol);
                if (filKolonneIndexMap.get(filnavn) == null)
                    filKolonneIndexMap.put(filnavn, new ArrayList<>());
                filKolonneIndexMap.get(filnavn).add(idx);
                if (idx == -1) throw new IllegalArgumentException("Kolonne '" + kol + "' fandtes ikke i " + outputfil);
                output.enheder.add(outputfil.enheder.get(idx));

                if (antalRækker != -1 && antalRækker != outputfil.data.size())
                    throw new IllegalStateException("Forventede " + antalRækker + " datarækker i " + outputfil);
                antalRækker = outputfil.data.size();
            }
        }

        // Lav datarækket
        for (int række = 0; række < antalRækker; række++) {
            String[] datalineE = new String[output.kolonnenavne.size()];
            int kolE = 0;
            for (String filnavn : ekstrakt.filKolonnerMap.keySet()) {
                CsvFile outputfil = outputMap.get(filnavn);
                for (int kol1 : filKolonneIndexMap.get(filnavn)) {
                    // Tag højde for at nogle af de sidste kolonner i en Daisy CSV fil kan være tomme
                    datalineE[kolE] = outputfil.data.get(række).length <= kol1 ? "" : outputfil.data.get(række)[kol1];
                    kolE++;
                }
            }
            output.data.add(datalineE);
        }
        return output;
    }

    public static void main(String[] args) {
        new CsvEkstraktor("crop.csv (year, month, mday, LAI), crop_prod.csv (year, month, mday, Crop AI, Leaf AI, Stem AI)", "xx");
        new CsvEkstraktor("crop.csv (*)", "xx");
        new CsvEkstraktor("crop.csv", "xx");
        new CsvEkstraktor("crop.csv");
    }
}
