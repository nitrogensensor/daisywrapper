package eu.nitrogensensor.daisylib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DaisyModel implements Cloneable {
    public Path directory;
    public String scriptFil;
    public String beskrivelse;
    private ArrayList<Erstatning> erstatninger = new ArrayList<>();
    private String id;

    public DaisyModel(String directory, String daisyInputfile) throws IOException {
        this(Paths.get(directory), daisyInputfile);
    }


    public DaisyModel(Path directory, String daisyInputfile) throws IOException {
        this.directory = directory;
        this.scriptFil = daisyInputfile;
        beskrivelse = daisyInputfile;
        id = UUID.randomUUID().toString();
    }

    public DaisyModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return id;
    }

    /** Opretter en kopi af en kørsel og kopi af dets erstatninger r*/
    public DaisyModel clon() {
        try {
            DaisyModel kopi = (DaisyModel) this.clone();
            kopi.erstatninger = (ArrayList<Erstatning>) erstatninger.clone();
            kopi.id = UUID.randomUUID().toString();
            return kopi;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /** Opretter en kopi af en kørsel og kopi af dets erstatninger
     * @param newFolder*/
    public DaisyModel toDirectory(Path newFolder) throws IOException {
        Utils.klonMappe(directory, newFolder);
        directory = newFolder;
        return this;
    }
    @Override
    public String toString() {
        return "Koersel{" +
                "orgMappe=" + directory +
                ", scriptFil='" + scriptFil + '\'' +
                '}';
    }


    public DaisyModel replace(String søgestreng, String erstatning) {
        erstatninger.add(new Erstatning(søgestreng, erstatning));
        return this;
    }


    public DaisyModel run() throws IOException {
        DaisyInvoker daisyInvoke = new DaisyInvoker();

        if (erstatninger.size()>0) {
            String scriptIndhold = new String(Files.readAllBytes(directory.resolve(scriptFil)));
            for (Erstatning e : erstatninger) scriptIndhold = e.erstat(scriptIndhold);
            Path scriptfilITmp = Files.createTempFile(directory, "replaced", scriptFil);
            Files.write(scriptfilITmp, scriptIndhold.getBytes());
            daisyInvoke.invokeDaisy(directory, directory.relativize(scriptfilITmp).toString());
            Files.delete(scriptfilITmp);
        } else {
            daisyInvoke.invokeDaisy(directory, scriptFil);
        }
        return this;
    }
}
