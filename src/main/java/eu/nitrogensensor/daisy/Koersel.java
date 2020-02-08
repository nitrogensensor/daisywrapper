package eu.nitrogensensor.daisy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Koersel {
    public final Path orgMappe;
    public final String scriptFil;
    private final ArrayList<Erstatning> erstatninger = new ArrayList<>();

    public Koersel(Path orgMappe, String scriptFil) {
        this.orgMappe = orgMappe;
        this.scriptFil = scriptFil;
    }

    /** Opretter en kopi af en kørsel og kopi af dets erstatninger */
    public Koersel(Koersel kørsel) {
        this(kørsel.orgMappe, kørsel.scriptFil);
        erstatninger.addAll(kørsel.erstatninger);
    }

    public void klargørTilMappe(Path destMappe) throws IOException {
        Utils.klonMappe(orgMappe, destMappe);

        String scriptIndholdOrg = new String(Files.readAllBytes(orgMappe.resolve(scriptFil)));

        String scriptIndhold = Erstatning.udfør(scriptIndholdOrg, erstatninger);

        // Overskriv scriptfil med den, hvor diverse felter er blevet erstattet
        Path scriptfilITmp = destMappe.resolve(scriptFil);
        Files.delete(scriptfilITmp);
        Files.write(scriptfilITmp, scriptIndhold.getBytes());
    }

    public Koersel erstat(String søgestreng, String erstatning) {
        erstatninger.add(new Erstatning(søgestreng, erstatning));
        return this;
    }
}
