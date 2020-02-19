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
    d.replace("(stop *)", "(stop 2019 8 31)"); // fuld kørsel
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

      for (int n=0; n<4; n++) {

        DaisyModel kørsel = d.clon()
                .setId(program+ String.format("_%02d", n))
                .replace("(run taastrup)", "(run Mark21 (column (\"" + program + "\")))");
        daisyModels.add(kørsel
                .toDirectory(Paths.get("daisy/run/remoteParTmp/" + kørsel.getId()))
        );
      }
    }


    ResultExtractor re = new ResultExtractor();
    re.addCsvExtractor("crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)", "crop-leaf-stem-AI.csv");
    re.addFile("harvest.csv");

    long tid = System.currentTimeMillis();
    // Lokale kørsler
    // Det tog 88,8 sek at køre 16 kørsler serielt
    //DaisyExecution.runSerial(daisyModels, re, Paths.get("daisy/run/serRes"));
    // Det tog 21,4 sek at køre 16 kørsler parrallel (8 kerner)
    //DaisyExecution.runParralel(daisyModels, re, Paths.get("daisy/run/parRes"));

    // Det tog 90,0 sek at køre 16 kørsler mod lokal server serielt
    // Det tog 121,3 sek at køre 16 kørsler mod Cloud Run serielt
    //DaisyRemoteExecution.runSerial(daisyModels, re, Paths.get("daisy/run/remoteRes"));

    // Det tog 22,9 sek at køre 16 kørsler parallelt i Cloud Run
    DaisyRemoteExecution.runParralel(daisyModels, re, Paths.get("daisy/run/remoteParRes"));

    System.out.printf("Det tog %.1f sek at køre %d kørsler\n", (System.currentTimeMillis()-tid)/1000.0, daisyModels.size());

  }
}
