package eu.nitrogensensor.daisylib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 Output af kald til Daisy kan caches og genbruges hvis der allerede findes en kørsel med præcist de samme inputs
 */
public class ExecutionCache {
    private final Path cacheplacering;
    private final HashMap<DaisyModel, String> daisyModelMd5Map = new HashMap<>();

    public ExecutionCache(Path cacheplacering) throws IOException {
        this.cacheplacering = cacheplacering;
        //this.cacheplacering = Paths.get(System.getenv("PWD"), cachemappe); // Hmm, ved ikke om det virker på Windows?
        Files.createDirectories(cacheplacering);
    }


    private Path findCachetResMappe(DaisyModel dm) throws IOException {
        String md5 = daisyModelMd5Map.get(dm);
        if (md5==null) {
            md5 = dm.md5sum();
            daisyModelMd5Map.put(dm, md5);
        }
        return cacheplacering.resolve(md5);
    }

    public boolean kanUdfyldesFraCache(DaisyModel dm) throws IOException {
        Path cachetResMappe = findCachetResMappe(dm);
        return (Files.exists(cachetResMappe));
    }

    public boolean udfyldFraCache(DaisyModel dm) throws IOException {
        boolean udfyldt;
        Path cachetResMappe = findCachetResMappe(dm);
        if (Files.exists(cachetResMappe)) {
            System.out.println(dm.getId() + " var allerede cachet i "+cachetResMappe.toAbsolutePath());
            dm.directory = cachetResMappe;
            //Utils.sletMappe(dm.directory);
            //Files.createSymbolicLink( dm.directory, cachetResMappe.toAbsolutePath() );
            //Utils.klonMappeViaLinks(cachetResMappe, dm.directory);
            udfyldt = true;
        } else {
            System.out.println(dm.getId() + " var IKKE cachet - den vil blive gemt i "+cachetResMappe);
            udfyldt = false;
        }
        return udfyldt;
    }

    public void gemICache(DaisyModel dm) throws IOException {
        String md5 = daisyModelMd5Map.get(dm);
        if (md5==null) throw new IllegalArgumentException(dm + " var ikke registreret - det skal den være, FØR en kørsel, for ellers kan jeg ikke lave md5");
        Path cachetResMappe = cacheplacering.resolve(md5);
        if (!Files.exists(cachetResMappe)) {
            System.out.println(dm.getId() + " er nu blevet cachet i "+cachetResMappe);
            Utils.klonMappeKopérAlt(dm.directory, cachetResMappe);
        }
    }
}
