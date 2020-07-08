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


    public void extract(Path fromDirectory, Path toDirectory) throws IOException {
        if (fromDirectory.equals(toDirectory) && kopiérFiler.size()>0) throw new IllegalArgumentException("Vil ikke kopiere "+kopiérFiler+" til og fra samme mappe:"+fromDirectory);
        //System.out.println("extract "+fromDirectory+ " "+toDirectory);

        Utils.sletMappe(toDirectory);
        Files.createDirectories(toDirectory);

        for (String fn : kopiérFiler) {
            Path fil = fromDirectory.resolve(fn);
            if (!Files.isDirectory(fil)) Files.copy(fromDirectory.resolve(fn), toDirectory.resolve(fn));
            else throw new IOException("Mapper er endnu ikke understøttet i kopiérFiler: "+kopiérFiler);
        }

        Map<String, CsvFile> readOutput = new LinkedHashMap<String, CsvFile>();
        for (String filnavn : outputfilnavne) {
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

    public void extract(Path fromDirectory, HashMap<String,String> fileContensMap) throws IOException {

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
        for (String filnavn : outputfilnavne) {
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

}
