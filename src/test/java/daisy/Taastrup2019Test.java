package daisy;


import eu.nitrogensensor.daisy.Erstatning;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;


public class Taastrup2019Test {


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