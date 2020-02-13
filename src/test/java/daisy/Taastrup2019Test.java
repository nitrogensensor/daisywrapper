package daisy;


import eu.nitrogensensor.daisylib.DaisyInvoker;
import eu.nitrogensensor.daisylib.Erstatning;
import eu.nitrogensensor.daisylib.DaisyModel;
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

        kørsel.outputEkstrakt.add(new DaisyModel.OutputEkstrakt("crop-leaf-stem-AI.csv", "crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)"));
        long tid = System.currentTimeMillis();
        Path tmpMappe = Files.createTempDirectory("ns-daisy");
        kørsel = kørsel.cloneToDirectory(tmpMappe);
        kørsel.klargørTilMappe2(tmpMappe);
        DaisyInvoker daisyInvoke = new DaisyInvoker();
        daisyInvoke.invokeDaisy(tmpMappe, scriptFil);
        kørsel.læsOutput(tmpMappe);
        for (DaisyModel.OutputEkstrakt ekstrakt1 : kørsel.outputEkstrakt) {
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
        DaisyModel.OutputEkstrakt ekstrakt = kørsel.outputEkstrakt.get(0);
        assertEquals("2015", ekstrakt.output.data.get(9)[0]);
        assertEquals("1", ekstrakt.output.data.get(9)[1]);
        assertEquals("10", ekstrakt.output.data.get(9)[2]);
        assertEquals("00.00", ekstrakt.output.data.get(9)[3]);


        Path fil = tmpMappe.resolve(ekstrakt.output.filnavn);
        ekstrakt.output.skrivDatafil(fil, ", ", "");
        assertTrue(Files.exists(fil));

        System.out.printf("Det tog %.1f sek\n", (System.currentTimeMillis()-tid)/1000.0);
//        Utils.sletMappe(tmpMappe);
    }

    @Test
    public void outputEkstrakt() {
        DaisyModel.OutputEkstrakt oe = new DaisyModel.OutputEkstrakt("xx", "crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)");
        assertEquals(oe.filKolonnerMap.get("crop.csv").get(0), "year");
        assertEquals(oe.filKolonnerMap.get("crop_prod.csv").get(1), "Leaf AI");
        new DaisyModel.OutputEkstrakt("xx", "crop.csv (*)");
        new DaisyModel.OutputEkstrakt("xx", "crop.csv");
        new DaisyModel.OutputEkstrakt("crop.csv");
    }

    @Test
    public void simpelErstatningVsRegex() throws IOException {
        String scriptFil = "Setup_DTU_Taastrup.dai";
        Path orgMappe = Paths.get("src/test/resources/Taastrup 2019/dtu_model");
        String scriptIndholdOrg = new String(Files.readAllBytes(orgMappe.resolve(scriptFil)));

        // sammenlign udkommet af simpel erstatning med regex erstatning
        String simpelRes = new Erstatning("(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")").erstat(scriptIndholdOrg);
        String regExpRes = new Erstatning("\\(path .+?\\)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")").erstat(scriptIndholdOrg);
        //System.out.println(regExpRes.substring(0,200));
        //System.out.println(simpelRes.substring(0,200));


        assertEquals(regExpRes,simpelRes);
    }

}