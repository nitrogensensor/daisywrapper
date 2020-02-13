package eu.nitrogensensor.daisy;


import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.DaisyModelExecution;
import eu.nitrogensensor.daisylib.csv.CsvEkstraktor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DaisyMain
{

  public static void main(String[] args) throws IOException, InterruptedException {
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
    d.run();

    String[] programmer = {
            "High_N_Low_W",
            "Low_N_Low_W",
            "Low_N_High_W",
            "High_N_High_W",
    };

    d.csvEkstraktor.add(new CsvEkstraktor("crop-leaf-stem-AI.csv", "crop.csv (year, month, mday, LAI), crop_prod.csv (Crop AI, Leaf AI, Stem AI)"));
    long tid = System.currentTimeMillis();


    ArrayList<DaisyModel> daisyModels = new ArrayList<>();
    for (String program : programmer) {
      Path tmpMappe = Paths.get("/tmp/p6/out_" + program); // Anden mappe i udviklingsøjemed
      DaisyModel kørsel = d.cloneToDirectory(tmpMappe);
      kørsel.replace("(run taastrup)", "(run Mark21 (column (\"" + program + "\")))");
      daisyModels.add(kørsel);
    }

    DaisyModelExecution.runParralel(daisyModels);

    for (DaisyModel kørsel : daisyModels) {
      kørsel.læsOutput(kørsel.directory);

      for (CsvEkstraktor ekstrakt1 : kørsel.csvEkstraktor) {
        ekstrakt1.lavUdtræk(kørsel.output);
      }

      for (CsvEkstraktor ekstrakt : kørsel.csvEkstraktor) {
        // Skriv outputfil med ekstrakt
        Path fil = kørsel.directory.resolve(ekstrakt.output.filnavn);
        String skilletegn = ", ";
        String header = "# Udtræk af "+ekstrakt.filKolonnerMap+" fra "+kørsel.scriptFil + "\n" +
                "# "+kørsel.beskrivelse + "\n";
        ekstrakt.output.skrivDatafil(fil, skilletegn, header);
      }

    }

    System.out.printf("Det tog %.1f sek\n", (System.currentTimeMillis()-tid)/1000.0);
  }
}
