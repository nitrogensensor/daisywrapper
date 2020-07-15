package eu.nitrogensensor.daisylib;


// Kilde: Jeppes arbejde

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

class DaisyInvoker {
    public void invokeDaisy(Path mappe, String inputFil) throws IOException {
        /*
        Jeppes properties-idé fejler hvis der recompileres, fordi JAR-filen slettes og en ny JAR dannes. Jacob 15 juli 2020.
        Properties prop = new Properties();
        InputStream input = DaisyInvoker.class.getClassLoader().getResourceAsStream("daisy.properties");
        prop.load(input);
         */
        String daisyPath = System.getenv("DAISY_PATH");
        if (daisyPath==null) {
            daisyPath = "/opt/daisy/bin/daisy";
            if (!new File(daisyPath).exists()) {
                System.err.println("Ingen Daisy i "+daisyPath+" og DAISY_PATH er ikke sat");
                daisyPath = "daisy";
            }
        }



        int exitValue;
        //System.out.println("RUN DAISY! "+mappe+" "+inputFil);
        //System.out.println(new File(prop.getProperty("daisy.executable.path")).getAbsolutePath());

        Process process = new ProcessBuilder(daisyPath, inputFil)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.to(mappe.resolve("daisyErr.log").toFile()))
//                .inheritIO()
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
            throw new RuntimeException("Something went wrong during execution of the Daisy script. mappe="+mappe+" inputFil="+inputFil);
    }
}
