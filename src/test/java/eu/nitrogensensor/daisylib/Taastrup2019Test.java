package eu.nitrogensensor.daisylib;


import eu.nitrogensensor.daisylib.csv.CsvEkstraktor;
import eu.nitrogensensor.daisylib.csv.CsvFile;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class Taastrup2019Test {


    @Test
    public void testEnFuldKørsel() throws IOException { // burde nok splittes op i stedet for at være jumbo-test. Tager 4 sekunder
        String scriptFil = "Setup_DTU_Taastrup.dai";
        Path orgMappe = Paths.get("src/test/resources/Taastrup 2019/dtu_model");
        DaisyModel kørsel = new DaisyModel(orgMappe, scriptFil);
        kørsel.replace("(stop *)", "(stop 2015 4 30)"); // for hurtigere kørsel
        kørsel.replace("(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")");
        kørsel.replace("(run taastrup)", "(run Mark21 (column (\"High_N_High_W\")))");

        Path daisyOutputmappe = Files.createTempDirectory("ns-daisy");
        kørsel = kørsel.clon().toDirectory(daisyOutputmappe);

        kørsel.run();
        ResultExtractor re = new ResultExtractor();
        re.addCsvExtractor("crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)", "crop-leaf-stem-AI.csv");
        re.addFile("crop.csv");
        Path ekstraktOutputmappe = Files.createTempDirectory("ns-daisy-ekstrakt");
        re.extract(daisyOutputmappe, ekstraktOutputmappe);

        // Linje 10 af "crop.csv" bør være
        // 2015    1       10      0       0       0       00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00
        CsvFile cropCsv = new CsvFile(ekstraktOutputmappe, "crop.csv");
        assertEquals("2015", cropCsv.data.get(9)[0]);
        assertEquals("1", cropCsv.data.get(9)[1]);
        assertEquals("10", cropCsv.data.get(9)[2]);
        assertEquals("0", cropCsv.data.get(9)[3]);


        // Linje 10 bør være
        // 2015, 1, 10, 00.00, 00.00, 00.00, 00.00
        CsvFile ekstrakt = new CsvFile(ekstraktOutputmappe, "crop-leaf-stem-AI.csv");
        assertEquals("2015", ekstrakt.data.get(9)[0]);
        assertEquals("1", ekstrakt.data.get(9)[1]);
        assertEquals("10", ekstrakt.data.get(9)[2]);
        assertEquals("00.00", ekstrakt.data.get(9)[3]);


        assertTrue(Files.exists(ekstraktOutputmappe.resolve("crop-leaf-stem-AI.csv")));

        Utils.sletMappe(daisyOutputmappe);
        Utils.sletMappe(ekstraktOutputmappe);
    }
}