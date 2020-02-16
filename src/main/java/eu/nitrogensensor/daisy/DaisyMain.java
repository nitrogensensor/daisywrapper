package eu.nitrogensensor.daisy;


import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.DaisyModelExecution;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.Utils;
import eu.nitrogensensor.executionservice.DaisyModelRemoteExecution;
import eu.nitrogensensor.executionservice.ExtractedContent;
import eu.nitrogensensor.executionservice.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DaisyMain
{

  public static void main(String[] args) throws IOException {
    System.out.println("hej fra DaisyMain");

    DaisyModel d = new DaisyModel("daisy/src/test/resources/Taastrup 2019/dtu_model", "Setup_DTU_Taastrup.dai");
    //System.out.println(d.getStarttime());
    //dry_bulk_density = d.Input['defhorizon'][0]['dry_bulk_density'].getvalue()
    //d.Input['defhorizon'][0]['dry_bulk_density'].setvalue(1.1*dry_bulk_density)
    //d.save_as(r'C:\Program Files\Daisy 5.72\exercises\Exercise01_new.dai')
    //DaisyModel.path_to_daisy_executable =  r'C:\Program Files\Daisy 5.72\bin\Daisy.exe'
    //d.run();

    d.replace("(stop *)", "(stop 2015 8 20)"); // for hurtigere kørsel
    //d.erstat("(stop *)", "(stop 2019 8 31)"); // fuld kørsel
    d.replace("(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")");
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
    //DaisyModelExecution.runSerial(daisyModels);
    //DaisyModelExecution.runParralel(daisyModels);

    Server.start();
    DaisyModelRemoteExecution.runSerial(daisyModels, re, Paths.get("daisy/run/remoteRes"));
    System.out.printf("Det tog %.1f sek\n", (System.currentTimeMillis()-tid)/1000.0);
    Server.stop();

    /* Lokale kørsler
    for (DaisyModel kørsel : daisyModels) {
      re.extract(kørsel.directory, Paths.get("daisy/run/res_"+kørsel.getId()));
    }

     */
  }
}
