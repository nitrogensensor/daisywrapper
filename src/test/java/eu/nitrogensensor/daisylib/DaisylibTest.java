package eu.nitrogensensor.daisylib;


import eu.nitrogensensor.daisylib.csv.CsvEkstraktor;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DaisylibTest {


    @Test
    public void outputEkstrakt() {
        CsvEkstraktor oe = new CsvEkstraktor("crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)", "xx");
        assertEquals(oe.filKolonnerMap.get("crop.csv").get(0), "year");
        assertEquals(oe.filKolonnerMap.get("crop_prod.csv").get(1), "Leaf AI");
        new CsvEkstraktor("crop.csv (*)", "xx");
        new CsvEkstraktor("crop.csv", "xx");
        new CsvEkstraktor("crop.csv");
    }

    @Test
    public void simpelErstatningVsRegex() throws IOException {
        String scriptFil = "Setup_DTU_Taastrup.dai";
        Path orgMappe = Paths.get("src/test/resources/Taastrup 2019/dtu_model");
        String scriptIndholdOrg = new String(Files.readAllBytes(orgMappe.resolve(scriptFil)));

        // sammenlign udkommet af simpel erstatning med regex erstatning
        String simpelRes = Erstatning.erstat(scriptIndholdOrg,"(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")");
        String regExpRes = Erstatning.erstat(scriptIndholdOrg,"\\(path .+?\\)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")");
        //System.out.println(regExpRes.substring(0,200));
        //System.out.println(simpelRes.substring(0,200));

        assertEquals(regExpRes,simpelRes);
    }

}