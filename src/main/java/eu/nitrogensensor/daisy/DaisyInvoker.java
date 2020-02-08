package eu.nitrogensensor.daisy;


// Kilde: Jeppes arbejde
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DaisyInvoker {
    public void invokeDaisy(String mappe, String inputFil) throws InterruptedException, IOException {
        Properties prop = new Properties();
        try(InputStream input = DaisyInvoker.class.getClassLoader().getResourceAsStream("daisy.properties")){
            if(input==null){
                System.out.println("No config properties were found");
                return;
            }
            prop.load(input);

        } catch (IOException e) {
            e.printStackTrace();
        }

        int exitValue;
        System.out.println("RUN DAISY!");
        System.out.println(new File(prop.getProperty("daisy.executable.path")).getAbsolutePath());

        Process process = new ProcessBuilder(new File(prop.getProperty("daisy.executable.path")).getAbsolutePath(), inputFil)
                .inheritIO()
                .directory(new File(mappe))
                .start();
        process.waitFor();
        exitValue = process.exitValue();
        process.destroy();

        if(exitValue != 0)
            throw new RuntimeException("Something went wrong during execution of the Daisy script.");
    }
}
