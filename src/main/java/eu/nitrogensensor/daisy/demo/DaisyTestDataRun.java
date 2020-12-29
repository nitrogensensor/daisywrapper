package eu.nitrogensensor.daisy.demo;


import eu.nitrogensensor.daisylib.DaisyExecution;
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

public class DaisyTestDataRun
{
  public static void main(String[] args) throws IOException {
    System.out.println("starter DaisyTestkoersel");

    DaisyModel d = new DaisyModel("src/test/resources/TestData", "Exercise01.dai");
    d.replace("(stop *)", "(stop 1995 1 1)");   // Set stop date
    //d.run();  // executes in the source directory - not recommended - use .copyToDirectory() first.


    ArrayList<DaisyModel> daisyModels = new ArrayList<>();
    for (double dry_bulk_density=1.40; dry_bulk_density<1.60; dry_bulk_density+=0.02) {
        // String dbd = String.format(Locale.US, "%.3f", dry_bulk_density);
        DaisyModel copy = d.createCopy()
                .setId("dbd_"+dry_bulk_density)
                // .copyToDirectory(Paths.get("tmp/local_result/dbd_"+dry_bulk_density)) // for local execution
                .replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density "+dry_bulk_density+" [g/cm^3])");
        daisyModels.add(copy);
    }

    // Local execution
    // DaisyExecution.runParralel(daisyModels);

    // Remote execution - optionally set up remote server endpoint
    // DaisyRemoteExecution.setRemoteEndpointUrl("http://nitrogen.saluton.dk:3210/");
    // DaisyRemoteExecution.setRemoteEndpointUrl("http://localhost:3210/");
    // DaisyRemoteExecution.setRemoteEndpointUrl("https://daisykoersel-6dl4uoo23q-lz.a.run.app");

    Map<String, ExtractedContent> results = DaisyRemoteExecution.runParralel(daisyModels, Paths.get("tmp/remote_result"));
    // Results are written to tmp/remote_result/dbd_xx/ but the file contents are also returned:
    System.out.println("results.keySet() = " + results.keySet());

    String id = daisyModels.get(0).getId();
    String soil_water_content = results.get(id).fileContensMap.get("Ex1/soil_water_content.dlf");
    System.out.println("soil_water_content = " + soil_water_content);
  }
}
