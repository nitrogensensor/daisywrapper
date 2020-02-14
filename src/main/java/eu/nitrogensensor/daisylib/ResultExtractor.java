package eu.nitrogensensor.daisylib;

import eu.nitrogensensor.daisylib.csv.CsvEkstraktor;
import eu.nitrogensensor.daisylib.csv.CsvFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ResultExtractor {
    public HashSet<String> outputfilnavne = new HashSet<>();

    public ArrayList<CsvEkstraktor> csvEkstraktors = new ArrayList<>();
    public ArrayList<String> kopiérFiler = new ArrayList<>();


    /**
     *
     * @param indhold "crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)"
     * @param skrivTilFilnavn "crop-leaf-stem-AI.csv"
     */
    public void addCsvExtractor(String indhold, String skrivTilFilnavn) {
        CsvEkstraktor csvEkstraktor = new CsvEkstraktor(indhold, skrivTilFilnavn);
        csvEkstraktors.add(csvEkstraktor);
        outputfilnavne.addAll(csvEkstraktor.filKolonnerMap.keySet());
    }

    public void addFile(String filename) {
        kopiérFiler.add(filename);
    }

    Map<String, CsvFile> readOutput = new LinkedHashMap<String, CsvFile>();

    public void extract(Path fromDirectory, Path toDirectory) throws IOException {
        if (fromDirectory.equals(toDirectory) && kopiérFiler.size()>0) throw new IllegalArgumentException("Vil ikke kopiere "+kopiérFiler+" til og fra samme mappe:"+fromDirectory);

        Utils.sletMappe(toDirectory);
        Files.createDirectories(toDirectory);

        for (String fn : kopiérFiler) {
            Files.copy(fromDirectory.resolve(fn), toDirectory.resolve(fn));
        }

        for (String filnavn : outputfilnavne) {
            CsvFile csvFile = new CsvFile(fromDirectory, filnavn);
            readOutput.put(filnavn, csvFile);
        }

        for (CsvEkstraktor ekstrakt1 : this.csvEkstraktors) {
            ekstrakt1.lavUdtræk(readOutput);
        }

        for (CsvEkstraktor ekstrakt : this.csvEkstraktors) {
            // Skriv outputfil med ekstrakt
            Path fil = toDirectory.resolve(ekstrakt.output.filnavn);
            String skilletegn = ", ";
            String header = "# Udtræk af "+ekstrakt.filKolonnerMap + "\n";
            ekstrakt.output.skrivDatafil(fil, skilletegn, header);
        }

    }

    public void extract(Path fromDirectory, HashMap<String,String> fileContensMap) throws IOException {

        for (String fn : kopiérFiler) {
            fileContensMap.put(fn, new String(Files.readAllBytes(fromDirectory.resolve(fn))));
        }

        for (String filnavn : outputfilnavne) {
            CsvFile csvFile = new CsvFile(fromDirectory, filnavn);
            readOutput.put(filnavn, csvFile);
        }

        for (CsvEkstraktor ekstrakt1 : this.csvEkstraktors) {
            ekstrakt1.lavUdtræk(readOutput);
        }

        // Generér HashMap med outputfiler med ekstrakt
        for (CsvEkstraktor ekstrakt : this.csvEkstraktors) {
            String skilletegn = ", ";
            String header = "# Udtræk af "+ekstrakt.filKolonnerMap + "\n";
            StringBuilder sb = new StringBuilder();
            ekstrakt.output.skrivData(sb, skilletegn, header);
            fileContensMap.put(ekstrakt.output.filnavn, sb.toString());
        }

    }
}
