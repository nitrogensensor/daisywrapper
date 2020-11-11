package eu.nitrogensensor.daisy;


import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.remote.DaisyRemoteExecution;
import eu.nitrogensensor.daisylib.remote.ExtractedContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;

public class DaisyTestkoersel2
{
  public static void main(String[] args) throws IOException {
    System.out.println("starter DaisyTestkoersel");

    // /home/j/Projekter/NitrogenSensor/daisy/PyDaisy
    DaisyModel d = new DaisyModel("daisy/src/test/resources/TestData", "Exercise01.dai");
    //d.copyToDirectory(Paths.get("/tmp/TestData"));
    d.replace("(stop *)", "(stop 1993 8 1)");   // Set stop date
    //d.run();


    ArrayList<DaisyModel> daisyModels = new ArrayList<>();
    for (double dry_bulk_density=1.40; dry_bulk_density<1.60; dry_bulk_density+=0.02) {
        DaisyModel kørsel = d.createCopy().setId("dbd_"+dry_bulk_density)
                .replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density "+dry_bulk_density+" [g/cm^3])");
        daisyModels.add(kørsel);
    }


    ResultExtractor re = new ResultExtractor();
    re.addFile("."); // hvis man vil have alle filer med tilbage

    long tid = System.currentTimeMillis();
    //DaisyExecution.runSerial(daisyModels, re, Paths.get("daisy/run/serRes"));
    //DaisyExecution.runParralel(daisyModels, re, Paths.get("daisy/run/parRes"));

    //DaisyRemoteExecution.runSerial(daisyModels, re, Paths.get("daisy/run/remoteRes"));
    DaisyRemoteExecution.setRemoteEndpointUrl("http://localhost:3210/");
    //DaisyRemoteExecution.setRemoteEndpointUrl("https://daisykoersel-6dl4uoo23q-lz.a.run.app");

    Map<String, ExtractedContent> results = DaisyRemoteExecution.runParralel(daisyModels);

    String soil_water_content = results.get("dbd_1.42").fileContensMap.get("Ex1/soil_water_content.dlf");
    System.out.println("soil_water_content = " + soil_water_content);
  }
}
