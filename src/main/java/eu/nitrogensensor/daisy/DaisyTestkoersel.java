package eu.nitrogensensor.daisy;


import eu.nitrogensensor.daisylib.DaisyExecution;
import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.remote.DaisyRemoteExecution;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DaisyTestkoersel
{
  public static void main(String[] args) throws IOException {
    System.out.println("starter DaisyTestkoersel");

    DaisyModel d = new DaisyModel("daisy/src/test/resources/Taastrup 2019/dtu_model", "Setup_DTU_Taastrup.dai");

    //d.replace("(stop *)", "(stop 2015 8 20)"); // for hurtigere kørsel
    //d.replace("(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")");
    //d.run();


    String[] programmer = {
            "High_N_Low_W",
            "Low_N_Low_W",
            "Low_N_High_W",
            "High_N_High_W",
    };
    ArrayList<DaisyModel> daisyModels = new ArrayList<>();
    for (String program : programmer) {

      for (int n=0; n<1000; n++) {

        DaisyModel kørsel = d.clon()
                .setId(program+ String.format("_%02d", n))
                .replace("(run taastrup)", "(run Mark21 (column (\"" + program + "\")))");
        daisyModels.add(kørsel
                .toDirectory(Paths.get("/tmp/daisy/run/remoteParTmp/" + kørsel.getId()))
        );
      }
    }


    ResultExtractor re = new ResultExtractor();
    re.addCsvExtractor("crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)", "crop-leaf-stem-AI.csv");
    re.addFile("harvest.csv");

    long tid = System.currentTimeMillis();
    //DaisyExecution.runSerial(daisyModels, re, Paths.get("daisy/run/serRes"));
    //DaisyExecution.runParralel(daisyModels, re, Paths.get("daisy/run/parRes"));
    System.out.printf("Det tog %.1f sek (eller %.1f min) at køre %d kørsler parrallelt (%d kerner)\n",
            (System.currentTimeMillis()-tid)/1000.0, (System.currentTimeMillis()-tid)/60000.0, daisyModels.size(), Runtime.getRuntime().availableProcessors());


    //DaisyRemoteExecution.runSerial(daisyModels, re, Paths.get("daisy/run/remoteRes"));
    DaisyRemoteExecution.runParralel(daisyModels, re, Paths.get("/tmp/daisy/run/remoteParRes"));

    System.out.printf("Det tog %.1f sek (%.1f min) at køre %d kørsler i Cloud Run - %d parrallelle instanser\n",
            (System.currentTimeMillis()-tid)/1000.0, (System.currentTimeMillis()-tid)/60000.0, daisyModels.size(), DaisyRemoteExecution.maxSamtidigeKørslerIgang);

/*
------------ SMÅ KØRSLER  ----------
Det tog 88,8 sek at køre 16 kørsler serielt

Det tog 18,1 sek (0,3 min) at køre 16 kørsler parrallel (8 kerner)
Det tog 21,4 sek at køre 16 kørsler parrallel (8 kerner)
Det tog 759,7 sek at køre 200 kørsler parrallel (8 kerner)
Det tog 90,0 sek at køre 16 kørsler mod lokal server serielt
Det tog 121,3 sek at køre 16 kørsler mod Cloud Run serielt

Det tog 22,9 sek at køre 16 kørsler parallelt i Cloud Run  (onsdag aften)
Det tog 93,3 sek at køre 200 kørsler parallelt i Cloud Run  (onsdag aften)
Det tog 144,4 sek sek at køre 200 kørsler parallelt i Cloud Run   (torsdag kl 10)
Det tog 664,9 sek at køre 2000 kørsler parallelt i Cloud Run - max 20 parrallel forespørgsler (onsdag aften)
Det tog 1300,8 sek at køre 2000 kørsler parallelt i Cloud Run - max 100 parrallel forespørgsler (torsdag kl 10)


Det tog 86,3 sek (1,4 min) at køre 200 kørsler i Cloud Run - max 31 parralelt
Det tog 63,0 sek (1,1 min) at køre 200 kørsler i Cloud Run - max 50 parralelt
Det tog 56,7 sek (0,9 min) at køre 200 kørsler i Cloud Run - max 100 parralelt
Det tog 59,1 sek (1,0 min) at køre 200 kørsler i Cloud Run - max 200 parralelt

Det tog 345,6 sek (5,8 min) at køre 2000 kørsler i Cloud Run



------------ FULDE KØRSLER ----------
Det tog 79,1 sek (1,3 min) at køre 20 kørsler parrallelt (8 kerner)
Det tog 144,2 sek for 40 kørsler parrallelt (8 kerner)


Det tog 32,2 sek (0,5 min) at køre 20 kørsler i Cloud Run
Det tog 52,6 sek (0,9 min) at køre 40 kørsler i Cloud Run
Det tog 66,8 sek (1,1 min) at køre 100 kørsler i Cloud Run
Det tog 131,3 sek (2,2 min) at køre 400 kørsler i Cloud Run
Det tog 764,1 sek (12,7 min) at køre 4000 kørsler i Cloud Run
 */
  }
}
