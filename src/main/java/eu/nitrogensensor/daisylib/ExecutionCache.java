package eu.nitrogensensor.daisylib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 Output af kald til Daisy kan caches og genbruges hvis der allerede findes en kørsel med præcist de samme inputs
 */
public class ExecutionCache {
    private final Path cacheplacering;
    private final HashMap<DaisyModel, String> daisyModelMd5Map = new HashMap<>();

    public ExecutionCache(String cachemappe) throws IOException {
        this.cacheplacering = Paths.get(cachemappe);
        Files.createDirectories(cacheplacering);
    }

    public Collection<DaisyModel> udfyldFraCache(Collection<DaisyModel> alle) throws Exception {
        ArrayList<DaisyModel> ikkeCachet = new ArrayList<>(alle);
        for (DaisyModel dm : alle) {
            String md5 = daisyModelMd5Map.get(dm);
            if (md5==null) daisyModelMd5Map.put(dm, md5 = Utils.md5sumMappe(dm.directory, dm.unikStreng()));
            Path cachetResMappe = cacheplacering.resolve(md5);
            if (Files.exists(cachetResMappe)) {
                System.out.println(dm + " var cachet i "+cachetResMappe);
                Utils.sletMappe(dm.directory);
                Files.createSymbolicLink( dm.directory, cachetResMappe.toAbsolutePath() );
                //Utils.klonMappeViaLinks(cachetResMappe, dm.directory);
                ikkeCachet.remove(dm);
            }
        }
        return ikkeCachet;
    }

    public void gemICache(Collection<DaisyModel> alle) throws IOException {
        for (DaisyModel dm : alle) {
            String md5 = daisyModelMd5Map.get(dm);
            if (md5==null) throw new IllegalArgumentException(dm + " var ikke registreret - det skal den være, FØR en kørsel, for ellers kan jeg ikke lave md5");
            Path cachetResMappe = cacheplacering.resolve(md5);
            if (!Files.exists(cachetResMappe)) {
                System.out.println(dm + " bliver cachet i "+cachetResMappe);
                Utils.klonMappeKopérAlt(dm.directory, cachetResMappe);
            }
        }
    }
}
