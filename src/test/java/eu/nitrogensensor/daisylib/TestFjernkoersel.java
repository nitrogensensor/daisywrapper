package eu.nitrogensensor.daisylib;


import eu.nitrogensensor.daisylib.remote.DaisyRemoteExecution;
import eu.nitrogensensor.daisylib.remote.ExtractedContent;
import eu.nitrogensensor.daisylib.remote.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Execution(ExecutionMode.CONCURRENT)
public class TestFjernkoersel {

    @BeforeAll
    public static void startServer() {
        Server.start(12354);
    }

    @AfterAll
    public static void lukServer() {
        Server.stop();
    }

    @Test
    public void parrallelFjernkørsel() throws IOException {
        DaisyModel kørsel = Taastrup2019Test.lavTaastrupKørsel();

        ArrayList<DaisyModel> arrayList = new ArrayList<>();
        arrayList.add(kørsel);
        arrayList.add(kørsel.createCopy());
        arrayList.add(kørsel.createCopy());

        ResultExtractor re = new ResultExtractor();
        re.addCsvExtractor("crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)", "crop-leaf-stem-AI.csv");
        re.addFile("crop.csv");

        DaisyRemoteExecution.setRemoteEndpointUrl(Server.getUrl());
        Map<String, ExtractedContent> res = DaisyRemoteExecution.runParralel(arrayList, re);
        String cropCsv = res.get(kørsel.getId()).fileContensMap.get("crop.csv");
        String cropLaiCsv = res.get(kørsel.getId()).fileContensMap.get("crop-leaf-stem-AI.csv");
        System.out.println(cropCsv);
        assertTrue(cropCsv.contains("Crop development and production"));
        System.out.println(cropLaiCsv);
        assertTrue(cropLaiCsv.split("2015")[1].startsWith(", 1, 1, 00.00, 00.00, 00.00, 00.00"));

        Utils.sletMappe(kørsel.directory);
    }


    @Test
    public void serielFjernkørsel() throws IOException {
        DaisyModel kørsel = Taastrup2019Test.lavTaastrupKørsel();

        ArrayList<DaisyModel> arrayList = new ArrayList<>();
        arrayList.add(kørsel);
//        arrayList.add(kørsel.clon());

        ResultExtractor re = new ResultExtractor();
        re.addCsvExtractor("crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)", "crop-leaf-stem-AI.csv");
        re.addFile("crop.csv");

        DaisyRemoteExecution.setRemoteEndpointUrl(Server.getUrl());
        ArrayList<ExtractedContent> res = DaisyRemoteExecution.runSerial(arrayList, re, Paths.get("/tmp/test_serielFjernkørsel"));
        String cropCsv = res.get(0).fileContensMap.get("crop.csv");
        String cropLaiCsv = res.get(0).fileContensMap.get("crop-leaf-stem-AI.csv");
        System.out.println(cropCsv);
        assertTrue(cropCsv.contains("Crop development and production"));
        System.out.println(cropLaiCsv);
        assertTrue(cropLaiCsv.split("2015")[1].startsWith(", 1, 1, 00.00, 00.00, 00.00, 00.00"));

        Utils.sletMappe(kørsel.directory);
    }

/*
    @Test
    public void serielFjernkørselUdeUdpakning() throws IOException {
        Server.PAK_UD_VED_MODTAGELSEN = false; // ikke så pålideligt da testsne kører parrallelt
        serielFjernkø_rsel();
        Server.PAK_UD_VED_MODTAGELSEN = true;
    }
*/
}