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

    Path tmpMappe = Files.createTempDirectory("work");
    tmpMappe =  Paths.get("/tmp/work");// Files.createTempDirectory("work2");

    FileUtils.klonMappe(orgMappe, tmpMappe);

    String indhold = new String(Files.readAllBytes(orgMappe.resolve(scriptFil)));

    Path scriptFilITmp = tmpMappe.resolve(scriptFil);
    Files.delete(scriptFilITmp);
    Files.write(scriptFilITmp, indhold.getBytes());


    daisyInvoke.invokeDaisy("daisy/src/test/resources/Taastrup 2019/dtu_model", "Setup_DTU_Taastrup.dai");

  }

  public void invokeDaisy(String mappe, String inputFil) throws InterruptedException, IOException {

  }

}
