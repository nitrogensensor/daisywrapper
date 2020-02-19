package eu.nitrogensensor.daisylib.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class CsvFile {
    public String filnavn;
    public String header;
    public ArrayList<String> kolonnenavne = new ArrayList<>();
    public ArrayList<String> enheder = new ArrayList<>();
    public ArrayList<String[]> data = new ArrayList<>();

    public CsvFile() {
    }

    public CsvFile(Path tmpMappe, String filnavn) throws IOException {
        this.filnavn = filnavn;
        Path fil = tmpMappe.resolve(filnavn);
        String csv = new String(Files.readAllBytes(fil));
        String[] csvsplit = csv.split("--------------------");
        this.header = csvsplit[0].trim();
        String[] linjer = csvsplit[1].trim().split("\n");
        if (this.kolonnenavne.size()!=0) throw new IllegalStateException("Outputfil er allerede parset");
        this.kolonnenavne.addAll(Arrays.asList(linjer[0].split("\t")));
        this.enheder.addAll(Arrays.asList(linjer[1].split("\t")));

        if (this.kolonnenavne.size() < this.enheder.size()) { // crop.csv har 24 kolonner, men 21 enheder (de sidste 3 kolonner er uden enhed), derfor < og ikke !=
            throw new IOException(fil + " har " + this.kolonnenavne.size() +" kolonner, men "+ this.enheder.size()+" enheder" +
                    "\nkol="+ this.kolonnenavne +"\nenh="+ this.enheder);
        }
        this.data = new ArrayList<>(linjer.length);
        for (int n=2; n<linjer.length; n++) {
            String[] linje = linjer[n].split("\t");
            if (this.kolonnenavne.size() < linje.length || linje.length < this.enheder.size()) { // data altid mellem
                throw new IOException(fil + " linje " + n +  " har " +linje.length +" kolonner, men "+
                        this.kolonnenavne.size() + " kolonnenavne og " + this.enheder.size() +" enheder" +
                        "\nlin="+ Arrays.toString(linje) +"\nkol="+ this.kolonnenavne +"\nenh="+ this.enheder);
            }
            this.data.add(linje);
        }
        // Fyld op med tomme enheder
        while (this.enheder.size()< this.kolonnenavne.size()) this.enheder.add("");
    }

    @Override
    public String toString() {
        return "Ouputfilindhold{" +
                "'" + filnavn + '\'' +
                ", " + kolonnenavne.size() +" kolonner"+
                ", " + (data==null?0:data.size())+" rækker" +
                '}';
    }


    static void printRække(String skilletegn, ArrayList<String> række, Appendable bufferedWriter) throws IOException {
        boolean førsteKolonne = true;
        for (String k : række) {
            if (!førsteKolonne) bufferedWriter.append(skilletegn);
            bufferedWriter.append(k);
            førsteKolonne = false;
        }
        bufferedWriter.append('\n');
    }

    static void printRække(String skilletegn, String[] række, Appendable bufferedWriter) throws IOException {
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
        skrivData(bufferedWriter, skilletegn, header);
        bufferedWriter.close();
    }

    public void skrivData(Appendable bufferedWriter, String skilletegn, String header) throws IOException {
        bufferedWriter.append(header);
        printRække(skilletegn, kolonnenavne, bufferedWriter);
        printRække(skilletegn, enheder, bufferedWriter);
        for (String[] datarække : data) {
            printRække(skilletegn, datarække, bufferedWriter);
        }
    }
}
