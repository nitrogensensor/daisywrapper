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
    //DaisyModel d = new DaisyModel("/home/j/Projekter/NitrogenSensor/daisy/PyDaisy/TestData", "Exercise01.dai");
    //DaisyModel d = new DaisyModel("/home/j/Projekter/NitrogenSensor/daisy/PyDaisy", "Exercise01.dai");

    DaisyModel d = new DaisyModel("daisy/src/test/resources/TestData", "Exercise01.dai");
    //d.copyToDirectory(Paths.get("/tmp/TestData"));
    d.replace("(stop *)", "(stop 1995 1 1)");   // Set stop date
    //d.run();


    ArrayList<DaisyModel> daisyModels = new ArrayList<>();
    for (double dry_bulk_density=1.40; dry_bulk_density<1.60; dry_bulk_density+=0.02) {
        DaisyModel model = d.createCopy()
                .setId("dbd_"+dry_bulk_density)
                .replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density "+dry_bulk_density+" [g/cm^3])");
        daisyModels.add(model);
    }


    //DaisyRemoteExecution.setRemoteEndpointUrl("http://localhost:3210/");
    //DaisyRemoteExecution.setRemoteEndpointUrl("https://daisykoersel-6dl4uoo23q-lz.a.run.app");

    Map<String, ExtractedContent> results = DaisyRemoteExecution.runParralel(daisyModels, Paths.get("tmp/remote_result"));
    System.out.println("results.keySet() = " + results.keySet());

    // TODO Mere intelligen måde at se resultat på, der ikke crachser hvis nøglen dbd_1.42 tilfæøldigvis ikke findes
    String soil_water_content = results.get("dbd_1.42").fileContensMap.get("Ex1/soil_water_content.dlf");
    System.out.println("soil_water_content = " + soil_water_content);
  }
}
