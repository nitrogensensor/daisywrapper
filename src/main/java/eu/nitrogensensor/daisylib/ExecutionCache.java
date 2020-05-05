package eu.nitrogensensor.daisylib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
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

    private static String md5sumMappe(Path mappe, String... ekstraData) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        //System.out.println("md5.update(" + Arrays.toString(ekstraData));
        for (String ekstra : ekstraData) md5.update(ekstra.getBytes());
        Files.walk(mappe).filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            //System.out.println("md5.update(" + mappe.relativize(path));
                            md5.update(mappe.relativize(path).toString().getBytes());
                            md5.update(Files.readAllBytes(path));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

        byte[] digest = md5.digest();
        String indkodet = Base64.getUrlEncoder().encodeToString(digest);
        if (!indkodet.endsWith("==")) throw new IllegalStateException("Troede altid at de endte med ==, men her er en uden?!?? "+indkodet);
        indkodet = indkodet.substring(0, indkodet.length()-2);
        //System.out.println("md5.digest() giver " + indkodet);
        return indkodet;
    }

    public Collection<DaisyModel> udfyldFraCache(Collection<DaisyModel> alle) throws Exception {
        ArrayList<DaisyModel> ikkeCachet = new ArrayList<>(alle);
        for (DaisyModel dm : alle) {
            String md5 = daisyModelMd5Map.get(dm);
            if (md5==null) daisyModelMd5Map.put(dm, md5 = md5sumMappe(dm.directory, dm.unikStreng()));
            Path cachetResMappe = cacheplacering.resolve(md5);
            if (Files.exists(cachetResMappe)) {
                System.out.println(dm + " var allerede cachet i "+cachetResMappe);
                Utils.sletMappe(dm.directory);
                Files.createSymbolicLink( dm.directory, cachetResMappe.toAbsolutePath() );
                //Utils.klonMappeViaLinks(cachetResMappe, dm.directory);
                ikkeCachet.remove(dm);
            } else {
                System.out.println(dm + " er ikke cachet - vil blive gemt i "+cachetResMappe);
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
                System.out.println(dm + " er nu blevet cachet i "+cachetResMappe);
                Utils.klonMappeKopérAlt(dm.directory, cachetResMappe);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("md5sumMappe(\"slamkode/src\") = " + md5sumMappe(Paths.get("slamkode/src")));
    }
}
