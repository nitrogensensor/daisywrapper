package eu.nitrogensensor.daisylib;


import eu.nitrogensensor.daisylib.csv.CsvFile;
import eu.nitrogensensor.daisylib.remote.DaisyRemoteExecution;
import eu.nitrogensensor.daisylib.remote.ExtractedContent;
import eu.nitrogensensor.daisylib.remote.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TestFjernkoersel {

    @BeforeClass
    public static void opsæt() {
        Server.start(12354);
    }

    @AfterClass
    public static void luk() {
        Server.stop();
    }

    @Test
    public void testFjernkørsel() throws IOException {
        String scriptFil = "Setup_DTU_Taastrup.dai";
        Path orgMappe = Paths.get("src/test/resources/Taastrup 2019/dtu_model");
        DaisyModel kørsel = new DaisyModel(orgMappe, scriptFil);
        kørsel.replace("(stop *)", "(stop 2015 3 30)"); // for hurtigere kørsel
        kørsel.replace("(run taastrup)", "(run Mark21 (column (\"High_N_High_W\")))");

        Path daisyOutputmappe = Files.createTempDirectory("ns-daisy");
        kørsel = kørsel.clon().toDirectory(daisyOutputmappe);

        ArrayList<DaisyModel> arrayList = new ArrayList<>();
        arrayList.add(kørsel);
        arrayList.add(kørsel.clon());
        arrayList.add(kørsel.clon());
        arrayList.add(kørsel.clon());

        ResultExtractor re = new ResultExtractor();
        re.addCsvExtractor("crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)", "crop-leaf-stem-AI.csv");
        re.addFile("crop.csv");

        ArrayList<ExtractedContent> res = DaisyRemoteExecution.runParralel(arrayList, re, null);
        String cropCsv = res.get(0).fileContensMap.get("crop.csv");
        String cropLaiCsv = res.get(0).fileContensMap.get("crop-leaf-stem-AI.csv");
        System.out.println(cropCsv);
        assertTrue(cropCsv.contains("Crop development and production"));
        System.out.println(cropLaiCsv);
        assertTrue(cropLaiCsv.split("2015")[1].startsWith(", 1, 1, 00.00, 00.00, 00.00, 00.00"));

        Utils.sletMappe(daisyOutputmappe);
    }
}