package eu.nitrogensensor.daisy;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DaisyMain
{

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("hej fra DaisyMain");

    DaisyInvoker daisyInvoke = new DaisyInvoker();

    String scriptFil = "Setup_DTU_Taastrup.dai";
    Path orgMappe = Paths.get("daisy/src/test/resources/Taastrup 2019/dtu_model");
    String scriptIndholdOrg = new String(Files.readAllBytes(orgMappe.resolve(scriptFil)));



    ArrayList<Erstatning> erstatninger = new ArrayList<>();
    erstatninger.add(new Erstatning("stop 2018 8 20", "stop 2015 8 20"));
//    erstatninger.add(new Erstatning("\\(path .+?\\)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")"));
    erstatninger.add(new SimpelErstatning("(path *)", "(path \"/opt/daisy/sample\" \"/opt/daisy/lib\" \".\" \"./common\")"));
    erstatninger.add(new SimpelErstatning("(defprogram taastrup batch*)","(defprogram taastrup batch (run (Mark21_lnlw)))"));

    String scriptIndhold = Erstatning.udfør(scriptIndholdOrg, erstatninger);

    Path tmpMappe = Paths.get("/tmp/work"); // Files.createTempDirectory("work");
    Utils.klonMappe(orgMappe, tmpMappe);

    // Overskriv scriptfil med den, hvor diverse felter er blevet erstattet
    Path scriptfilITmp = tmpMappe.resolve(scriptFil);
    Files.delete(scriptfilITmp);
    Files.write(scriptfilITmp, scriptIndhold.getBytes());


    daisyInvoke.invokeDaisy(tmpMappe, scriptFil );//daisy/src/test/resources/Taastrup 2019/dtu_model", "Setup_DTU_Taastrup.dai");

  }

  public void invokeDaisy(String mappe, String inputFil) throws InterruptedException, IOException {

  }

}
