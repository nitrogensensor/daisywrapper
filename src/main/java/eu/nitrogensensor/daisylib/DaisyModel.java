package eu.nitrogensensor.daisylib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DaisyModel implements Cloneable {
    public Path directory;
    public String scriptFil;
    private String scriptIndhold;
    private boolean scriptIndholdÆndret;
    public String beskrivelse;

    public DaisyModel(String directory, String daisyInputfile) throws IOException {
        this(Paths.get(directory), daisyInputfile);
    }


    public DaisyModel(Path directory, String daisyInputfile) throws IOException {
        this.directory = directory;
        this.scriptFil = daisyInputfile;
        scriptIndhold = new String(Files.readAllBytes(directory.resolve(scriptFil)));
    }

    /** Opretter en kopi af en kørsel og kopi af dets erstatninger
     * @param newFolder*/
    public DaisyModel cloneToDirectory(Path newFolder) throws IOException {
        if (!output.isEmpty()) throw new IllegalStateException("Du bør ikke bruge en kørsel der allerede har output som skabelon for en anden kørsel");
        try {
            DaisyModel kopi = (DaisyModel) this.clone();
            kopi.outputEkstrakt = new ArrayList<OutputEkstrakt>();
            for (OutputEkstrakt ekstrakt : outputEkstrakt) {
                kopi.outputEkstrakt.add(new OutputEkstrakt(ekstrakt));
            }
            kopi.output = new LinkedHashMap<String, Ouputfilindhold>();
            kopi.directory = newFolder;
            Utils.klonMappe(directory, kopi.directory);
            return kopi;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Koersel{" +
                "orgMappe=" + directory +
                ", scriptFil='" + scriptFil + '\'' +
                ", outputfilnavne=" + outputEkstrakt +
                ", output=" + output +
                '}';
    }


    public DaisyModel replace(String søgestreng, String erstatning) {
        scriptIndhold = Erstatning.erstat(this.scriptIndhold, søgestreng, erstatning);
        scriptIndholdÆndret = true;
        return this;
    }


    public void læsOutput(Path tmpMappe) throws IOException {
        HashSet<String> outputfilnavne = new HashSet<>();
        for (OutputEkstrakt outputEkstrakt : outputEkstrakt) {
            outputfilnavne.addAll(outputEkstrakt.filKolonnerMap.keySet());
        }
        for (String filnavn : outputfilnavne) {
            Ouputfilindhold ouputfilindhold = new Ouputfilindhold(tmpMappe, filnavn);
            output.put(filnavn, ouputfilindhold);
        }
    }

    public void run() throws IOException {
        DaisyInvoker daisyInvoke = new DaisyInvoker();

        if (scriptIndholdÆndret) {
            Path scriptfilITmp = Files.createTempFile(directory, "replaced", scriptFil);
            Files.write(scriptfilITmp, scriptIndhold.getBytes());
            daisyInvoke.invokeDaisy(directory, scriptfilITmp.toString());
            Files.delete(scriptfilITmp);
        } else {
            daisyInvoke.invokeDaisy(directory, scriptFil);
        }
    }


    public Map<String, Ouputfilindhold> output = new LinkedHashMap<String, Ouputfilindhold>();

    public ArrayList<OutputEkstrakt> outputEkstrakt = new ArrayList<>();
}