package eu.nitrogensensor.daisylib;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {
    /**
     * Kloner en mappe rekursivt - en kopi af mappestrukturen laves, og fyldes med symbolske links til den oprindelige mappe
     *
     * @param fraMappe Mappen, der skal kopieres
     * @param tilMappe Destination. Indhold i mappen overskrives hvis det allerede findes
     */
    public static void klonMappe(Path fraMappe, Path tilMappe) throws IOException {
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

    public static void sletMappe(Path tilMappe) throws IOException {
        if (!Files.exists(tilMappe)) return;
        // Slet mappe
        Files.walk(tilMappe)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }


    // Kilde: https://stackoverflow.com/questions/15968883/how-to-zip-a-folder-itself-using-java/32052016#32052016
    public static void zipMappe(String inputMappe, OutputStream os) throws IOException {
        Path pp = Paths.get(inputMappe);
        try (ZipOutputStream zs = new ZipOutputStream(os);
             Stream<Path> paths = Files.walk(pp)) {
            paths
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        }

    }


    // Kilde: https://mkyong.com/java/how-to-decompress-files-from-a-zip-file/
    public static void unzipMappe(InputStream inputStream, String outputMappe) throws IOException {

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
            System.out.println("file unzip : " + newFile.getAbsoluteFile());
            new File(newFile.getParent()).mkdirs();

            FileOutputStream fos = new FileOutputStream(newFile);

            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

            fos.close();
            ze = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();

        System.out.println("Done");
    }

    public static void main(String[] args) throws IOException {
        OutputStream os = Files.newOutputStream(Paths.get("slamkode.zip"));
        zipMappe("slamkode/src", os);
        os.close();

        unzipMappe(new FileInputStream("slamkode.zip"), "/tmp/");

    }
}
