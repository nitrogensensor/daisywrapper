package eu.nitrogensensor.daisylib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DaisyModel implements Cloneable {
    public Path directory;
    public String scriptFil;
    public String beskrivelse;
    private ArrayList<Erstatning> erstatninger = new ArrayList<>();
    private String id;

    /**
     * @return en unik streng, der beskriver hvilken scriptFil der er tale om, samt alle de erstatninger der sker i scriptfilen inden kørslen
     */
    private String unikStreng() {
        StringBuilder sb = new StringBuilder(erstatninger.size()*100 + scriptFil.length());
        sb.append(scriptFil); // navnet på scriptfilen
        for (Erstatning erstatning : erstatninger) sb.append(erstatning.unikStreng());
        return sb.toString();
    }

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
    public DaisyModel createCopy() {
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
    public DaisyModel copyToDirectory(Path newFolder) throws IOException {
        Utils.klonMappeViaLinks(directory, newFolder);
        directory = newFolder;
        return this;
    }

    @Override
    public String toString() {
        String erstStr = String.valueOf(erstatninger);
        if (erstStr.length() > 360) erstStr = erstStr.substring(0, 355)+"...";
        return "DaisyModel{" + directory +
                " '" + scriptFil + '\'' +
                ", id='" + id + '\'' +
                ", erstatninger=" + erstStr +
                '}';
    }

    public DaisyModel replace(String søgestreng, String erstatning) {
        return replace(søgestreng, erstatning, true);
    }


    public DaisyModel replace(String søgestreng, String erstatning, boolean præcisÉnGang) {
        erstatninger.add(new Erstatning(søgestreng, erstatning, præcisÉnGang));
        return this;
    }

    public DaisyModel run() throws IOException {
        DaisyInvoker daisyInvoke = new DaisyInvoker();

        if (erstatninger.size()>0) {
            String scriptIndhold = new String(Files.readAllBytes(directory.resolve(scriptFil)));
            for (Erstatning e : erstatninger) scriptIndhold = e.erstat(scriptIndhold);
            // Path scriptfilMedErstatninger = Files.createTempFile(directory, "replaced", scriptFil); // skal være læsbar for alle!!!
            Path scriptfilMedErstatninger = directory.resolve("replaced_" + id +"_"+ Integer.toString(erstatninger.hashCode(), Character.MAX_RADIX)+"_"+ scriptFil);
            while (Files.exists(scriptfilMedErstatninger)) {
                new IllegalStateException(scriptfilMedErstatninger+" fandtes allerede - finder et unikt ID").printStackTrace();
                id = Integer.toString((int) (Math.random()*Integer.MAX_VALUE), Character.MAX_RADIX);
            }
            Files.write(scriptfilMedErstatninger, scriptIndhold.getBytes());
            daisyInvoke.invokeDaisy(directory, directory.relativize(scriptfilMedErstatninger).toString());
            //Files.delete(scriptfilMedErstatninger);
        } else {
            daisyInvoke.invokeDaisy(directory, scriptFil);
        }
        return this;
    }

    /**
     * Finder en unik md5-sum for den jomfruelige kørsel som helhed (mappen, scriptfilen og erstatningerne).
     * Summen ændrer sig afhængig af mappens indhold, de erstatninger der sker i mappen under kørsel, samt hvilken scriptFil der køres.
     * Metoden er kun nyttig hvis den kaldes FØR kørslen af Daisy faktisk sker - efter kørslen er mappen 'forurenet' med logfiler med tidsstempler i og vil derfor altid afvige fra summen i en jombruelig kørsel
     * @return En unik streng for den samlede (jomfruelige) kørsel
     */
    String md5sum() throws IOException {
        String unikStreng = this.unikStreng();
        String md5sum = md5sumMappe(this.directory, unikStreng);
        return md5sum;
    }

    private static String md5sumMappe(Path mappe, String... ekstraData) throws IOException {
        try {
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
        } catch (NoSuchAlgorithmException e) { // burde ikke ske
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("md5sumMappe(\"slamkode/src\") = " + md5sumMappe(Paths.get("slamkode/src")));
    }
}
