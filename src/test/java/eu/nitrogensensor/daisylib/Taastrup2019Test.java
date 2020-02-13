package eu.nitrogensensor.daisylib;


import eu.nitrogensensor.daisylib.*;
import eu.nitrogensensor.daisylib.csv.CsvEkstraktor;
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

        kørsel.csvEkstraktor.add(new CsvEkstraktor("crop-leaf-stem-AI.csv", "crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)"));
        long tid = System.currentTimeMillis();
        Path tmpMappe = Files.createTempDirectory("ns-daisy");
        kørsel = kørsel.cloneToDirectory(tmpMappe);

        kørsel.run();
        kørsel.læsOutput(tmpMappe);
        for (CsvEkstraktor ekstrakt1 : kørsel.csvEkstraktor) {
            ekstrakt1.lavUdtræk(kørsel.output);
        }

        // Linje 10 af "crop.csv" bør være
        // 2015    1       10      0       0       0       00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00   00.00
        assertEquals("2015", kørsel.output.get("crop.csv").data.get(9)[0]);
        assertEquals("1", kørsel.output.get("crop.csv").data.get(9)[1]);
        assertEquals("10", kørsel.output.get("crop.csv").data.get(9)[2]);
        assertEquals("0", kørsel.output.get("crop.csv").data.get(9)[3]);


        // Linje 10 bør være
        // 2015, 1, 10, 00.00, 00.00, 00.00, 00.00
        CsvEkstraktor ekstrakt = kørsel.csvEkstraktor.get(0);
        assertEquals("2015", ekstrakt.output.data.get(9)[0]);
        assertEquals("1", ekstrakt.output.data.get(9)[1]);
        assertEquals("10", ekstrakt.output.data.get(9)[2]);
        assertEquals("00.00", ekstrakt.output.data.get(9)[3]);


        Path fil = tmpMappe.resolve(ekstrakt.output.filnavn);
        ekstrakt.output.skrivDatafil(fil, ", ", "");
        assertTrue(Files.exists(fil));

        System.out.printf("Det tog %.1f sek\n", (System.currentTimeMillis()-tid)/1000.0);
        Utils.sletMappe(tmpMappe);
    }
}