package eu.nitrogensensor.daisylib;


// Kilde: Jeppes arbejde
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public class DaisyInvoker {
    public void invokeDaisy(Path mappe, String inputFil) throws IOException {
        Properties prop = new Properties();
        InputStream input = DaisyInvoker.class.getClassLoader().getResourceAsStream("daisy.properties");
        prop.load(input);

        int exitValue;
        System.out.println("RUN DAISY!");
        System.out.println(new File(prop.getProperty("daisy.executable.path")).getAbsolutePath());

        Process process = new ProcessBuilder(new File(prop.getProperty("daisy.executable.path")).getAbsolutePath(), inputFil)
                .inheritIO()
                .directory(mappe.toFile())
                .start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        exitValue = process.exitValue();
        process.destroy();

        if(exitValue != 0)
            throw new RuntimeException("Something went wrong during execution of the Daisy script.");
    }
}
