package eu.nitrogensensor.daisylib;

import eu.nitrogensensor.daisylib.csv.CsvEkstraktor;
import eu.nitrogensensor.daisylib.csv.CsvFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ResultExtractor {
    public boolean cleanCsvOutput;
    private HashSet<String> csvOutputfilnavne = new HashSet<>();

    private ArrayList<CsvEkstraktor> csvEkstraktors = new ArrayList<>();
    private ArrayList<String> kopiérFiler = new ArrayList<>();


    /**
     *
     * @param indhold "crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)"
     * @param skrivTilFilnavn "crop-leaf-stem-AI.csv"
     */
    public void addCsvExtractor(String indhold, String skrivTilFilnavn) {
        CsvEkstraktor csvEkstraktor = new CsvEkstraktor(indhold, skrivTilFilnavn);
        csvEkstraktors.add(csvEkstraktor);
        csvOutputfilnavne.addAll(csvEkstraktor.filKolonnerMap.keySet());
    }

    public ResultExtractor addFile(String filename) {
        kopiérFiler.add(filename);
        return this;
    }


    public void extractToDirectory(Path fromDirectory, Path toDirectory) throws IOException {
        if (fromDirectory.equals(toDirectory) && kopiérFiler.size()>0) throw new IllegalArgumentException("Vil ikke kopiere "+kopiérFiler+" til og fra samme mappe:"+fromDirectory);
        //System.out.println("extract "+fromDirectory+ " "+toDirectory);

        Utils.sletMappe(toDirectory);
        Files.createDirectories(toDirectory);

        // En tom extractor giver ikke mening - i så fald kopieres hele mappen
        if (csvEkstraktors.isEmpty() && kopiérFiler.isEmpty()) addFile(".");

        for (String fn : kopiérFiler) {
            Path fil = fromDirectory.resolve(fn);
            if (!Files.isDirectory(fil)) Files.copy(fromDirectory.resolve(fn), toDirectory.resolve(fn));
            else try (Stream<Path> stream = Files.walk(fil)) {
                stream.forEach(fra -> {
                    if (!Files.isDirectory(fra)) try {
                        Path til = toDirectory.resolve(fromDirectory.relativize(fra));
                        Files.createDirectories(til.getParent());
                        Files.copy(fra, til);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage(), e);
                    }
                });
            }
        }

        Map<String, CsvFile> readOutput = new LinkedHashMap<String, CsvFile>();
        for (String filnavn : csvOutputfilnavne) {
            CsvFile csvFile = new CsvFile(fromDirectory, filnavn);
            readOutput.put(filnavn, csvFile);
        }

        for (CsvEkstraktor csvEkstraktor : this.csvEkstraktors) {
            CsvFile output = csvEkstraktor.lavUdtræk(readOutput);
            // Skriv outputfil med ekstrakt
            Path fil = toDirectory.resolve(output.filnavn);
            String skilletegn = "\t";
            String header = "# Udtræk af "+csvEkstraktor.filKolonnerMap + "\n";
            output.skrivDatafil(fil, skilletegn, header);
        }
    }

    public void extractToHashMap(Path fromDirectory, HashMap<String,String> fileContensMap) throws IOException {

        // En tom extractor giver ikke mening - i så fald kopieres hele mappen
        if (csvEkstraktors.isEmpty() && kopiérFiler.isEmpty()) addFile(".");

        for (String fn : kopiérFiler) {
            Path fil = fromDirectory.resolve(fn);
            if (!Files.isDirectory(fil)) fileContensMap.put(fn, new String(Files.readAllBytes(fil)));
            else try (Stream<Path> stream = Files.walk(fil)) {
                stream.forEach(fra -> {
                    if (!Files.isDirectory(fra)) try {
                        Path til = fromDirectory.relativize(fra);
                        fileContensMap.put(til.toString(), new String(Files.readAllBytes(fra)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage(), e);
                    }
                });
            }
        }

        Map<String, CsvFile> readOutput = new LinkedHashMap<String, CsvFile>();
        for (String filnavn : csvOutputfilnavne) {
            CsvFile csvFile = new CsvFile(fromDirectory, filnavn);
            readOutput.put(filnavn, csvFile);
        }
        // Generér HashMap med outputfiler med ekstrakt
        for (CsvEkstraktor ekstrakt : this.csvEkstraktors) {
            CsvFile output = ekstrakt.lavUdtræk(readOutput);
            String skilletegn = ", ";
            String header = "# Udtræk af "+ekstrakt.filKolonnerMap + "\n";
            StringBuilder sb = new StringBuilder();
            output.skrivData(sb, skilletegn, header);
            fileContensMap.put(output.filnavn, sb.toString());
        }

    }

    /**
     * Tjekker at der ikke findes nogen af outputfilerne i inputmappe.
     * Da vi bruger links i stedet for at kopiere filerne fysisk når vi kloner en mappe kan det
     * lede til inkonsistenser hvor flere Daisy-kørsler via hvert sit link skriver ned i den samme outputfil.
     * @param inputDir inputmappen
     * @throws IOException Hvis nogen af outputfilerne findes i inputmappe
     */
    public void tjekResultatIkkeAlleredeFindes(Path inputDir) throws IOException {
        for (String f : kopiérFiler) _tjekFindesIkke(inputDir, f);
        for (CsvEkstraktor csv : csvEkstraktors) {
            _tjekFindesIkke(inputDir, csv.skrivTilFilnavn);
            for (String f : csv.filKolonnerMap.keySet()) _tjekFindesIkke(inputDir, f);
        }
    }


    private void _tjekFindesIkke(Path inputDir, String f) throws IOException {
        File fil = inputDir.resolve(f).toFile();
        if (fil.exists() && !fil.isDirectory()) {
            throw new IOException("Outputfil "+f+" findes allede i inputmappe: "+inputDir+". Det kan lede til inkonsistenser i output og skal derfor undgås.");
        }
    }

    public static void cleanCsv(HashMap<String, String> fileContensMap) {
        for (Map.Entry<String, String> fil : fileContensMap.entrySet()) {
            if (!fil.getKey().endsWith(".csv")) continue;
            String csv = fil.getValue();
            String[] csvsplit = csv.split("--------------------\n");
            if (csvsplit.length<2) continue;
            String rest = csvsplit[1];
            int n1 = rest.indexOf('\n');
            int n2 = rest.indexOf('\n', n1+1);
            csv = rest.substring(0, n1) + rest.substring(n2);
            fil.setValue(csv);
        }
    }
}
