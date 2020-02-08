package eu.nitrogensensor.daisy;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DaisyMain
{

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("hej fra DaisyMain");

    DaisyInvoker daisyInvoke = new DaisyInvoker();

    String scriptFil = "Setup_DTU_Taastrup.dai";
    Path orgMappe = Paths.get("daisy/src/test/resources/Taastrup 2019/dtu_model");
    String scriptIndholdOrg = new String(Files.readAllBytes(orgMappe.resolve(scriptFil)));


    String scriptIndhold = Utils.erstat(scriptIndholdOrg, "stop 2018 8 20", "stop 2015 8 20");

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
