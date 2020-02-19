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
      DaisyModel kørsel = d.clon()//.toDirectory(Paths.get("daisy/run/tmp/out_" + program))
              .setId(program)
              .replace("(run taastrup)", "(run Mark21 (column (\"" + program + "\")))");
      daisyModels.add(kørsel);
    }


    ResultExtractor re = new ResultExtractor();
    re.addCsvExtractor("crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)", "crop-leaf-stem-AI.csv");
    re.addFile("harvest.csv");

    long tid = System.currentTimeMillis();
    // Lokale kørsler
    //DaisyExecution.runSerial(daisyModels, re, Paths.get("daisy/run/serRes"));
    DaisyExecution.runParralel(daisyModels, re, Paths.get("daisy/run/parRes"));


    //DaisyRemoteExecution.runSerial(daisyModels, re, Paths.get("daisy/run/remoteResHurra"));
    System.out.printf("Det tog %.1f sek\n", (System.currentTimeMillis()-tid)/1000.0);

  }
}
