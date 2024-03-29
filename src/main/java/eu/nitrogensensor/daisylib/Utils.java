package eu.nitrogensensor.daisylib;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {
    public static boolean debug = true;

    /**
     * Kloner en mappe rekursivt - en kopi af mappestrukturen laves, og fyldes med symbolske links til den oprindelige mappe
     *
     * @param fraMappe Mappen, der skal kopieres
     * @param tilMappe Destination. Indhold i mappen overskrives hvis det allerede findes
     */
    public static void klonMappeViaLinks(Path fraMappe, Path tilMappe) throws IOException {
        final Path fra = fraMappe.toAbsolutePath(); // Fuld sti
        sletMappe(tilMappe);

        AtomicReference<IOException> fejl = new AtomicReference<>(); // Hvis der opstår en exception skal den kastes videre
        Files.walk(fra).forEach(fraFil -> {
            try {
                if (fejl.get()!=null) return;
                Path tilFil = tilMappe.resolve(fra.relativize(fraFil));
                if( Files.isDirectory(fraFil)) {
                    if(!Files.exists(tilFil)) Files.createDirectories(tilFil);
                    return;
                }
                Files.deleteIfExists(tilFil);
                //Files.copy( s, d );
                Files.createSymbolicLink( tilFil, fraFil );
            } catch( IOException e ) {
                e.printStackTrace();
                fejl.set(e);
            } catch( Exception e ) {
                e.printStackTrace();
                fejl.set( new IOException(e));
            }
        });
        if (fejl.get()!=null) throw fejl.get();
    }

    /**
     * Kloner en mappe rekursivt - en kopi af mappestrukturen laves, og fyldes med kopier af filerne i den oprindelige mappe
     *
     * @param fraMappe Mappen, der skal kopieres
     * @param tilMappe Destination. Indhold i mappen overskrives hvis det allerede findes
     */
    static void klonMappeKopérAlt(Path fraMappe, Path tilMappe) throws IOException {
        sletMappe(tilMappe);
        try (Stream<Path> stream = Files.walk(fraMappe)) {
            stream.forEach(fra -> {
                try {
                    Path til = tilMappe.resolve(fraMappe.relativize(fra));
                    Files.createDirectories(til.getParent());
                    Files.copy(fra, til);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            });
        }
    }

    public static void sletMappe(Path tilMappe) throws IOException {
        if (!Files.exists(tilMappe)) return;
        // Slet mappe
        Files.walk(tilMappe)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        if (Files.exists(tilMappe)) Files.delete(tilMappe); // nødvendig?
    }

/*
        ArrayList<Path> filer = new ArrayList<Path>();
        Files.walk(inputDir).filter(fraFil -> !Files.isDirectory(fraFil)).forEach(f -> filer.add(f));
        if (FEJLFINDING) System.out.println("filer="+filer);
 */

    // Kilde: https://stackoverflow.com/questions/15968883/how-to-zip-a-folder-itself-using-java/32052016#32052016
    public static void zipMappe(String inputMappe, OutputStream os) throws IOException {
        Path pp = Paths.get(inputMappe);
        try (ZipOutputStream zs = new ZipOutputStream(os);
             Stream<Path> paths = Files.walk(pp)) {
            paths
                    // Tomme mapper skal også med - f.eks en tom Output-mappe som Daisy-scriptsne antager findes og Daisy desværre ikke opretter selv
                    //.filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            if (Files.isDirectory(path)) { // Tomme mapper skal også med - f.eks en tom output-mappe hvor Daisy skal lægge sit resultat
                                ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString()+"/");
                                zs.putNextEntry(zipEntry);
                                zs.closeEntry();
                            } else {
                                ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString().replace(File.separatorChar,'/'));
                                zs.putNextEntry(zipEntry);
                                Files.copy(path, zs);
                                zs.closeEntry();
                            }
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        }
    }


    // Kilde: https://mkyong.com/java/how-to-decompress-files-from-a-zip-file/
    public static void unzipMappe(InputStream inputStream, String outputMappe) throws IOException {
        if (debug) System.out.println("unzipMappe  " + outputMappe);

        byte[] buffer = new byte[1024];

        File folder = new File(outputMappe);
        if (!folder.exists()) {
            folder.mkdir();
        }

        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String fileName = ze.getName();
            File newFile = new File(outputMappe + File.separator + fileName);
            if (ze.isDirectory()) {
                newFile.mkdirs();
            } else {
                if (debug) System.out.println("file unzip : " + newFile.getAbsoluteFile());
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            }
            ze = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static void main(String[] args) throws Exception {
        OutputStream os = Files.newOutputStream(Paths.get("slamkode.zip"));
        zipMappe("slamkode/src", os);
        os.close();
        unzipMappe(new FileInputStream("slamkode.zip"), "/tmp/");
    }


    private static void skrivZip() throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");

        URI uri = URI.create("jar:file:/tmp/zipfstest.zip");
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path externalTxtFile = Paths.get("README.md");
            Path pathInZipfile = zipfs.getPath("README.md");
            // Copy a file into the zip file
            Files.copy(externalTxtFile, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String klipStreng(Object data, int maxLgd) {
        String str = data==null?"null":data.toString();
        if (str.length()<=maxLgd) return str;
        return str.substring(0, maxLgd-3)+"...";
    }
}
