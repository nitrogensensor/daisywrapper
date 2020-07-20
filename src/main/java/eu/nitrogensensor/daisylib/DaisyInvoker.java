package eu.nitrogensensor.daisylib;


// Kilde: Jeppes arbejde

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        Path daisyErr = mappe.resolve("daisyErr.log");

        Process process = new ProcessBuilder(daisyPath, inputFil)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.to(daisyErr.toFile()))
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

        if(exitValue != 0) {
            List<String> fejllinjer = Files.readAllLines(daisyErr);
            if (fejllinjer.size()>5) fejllinjer = fejllinjer.subList(fejllinjer.size()-5, fejllinjer.size());
            throw new IOException("Daisy error. mappe="+mappe+" inputFil="+inputFil+fejllinjer+"\n"+String.join("\n", fejllinjer));
        }
    }
}
