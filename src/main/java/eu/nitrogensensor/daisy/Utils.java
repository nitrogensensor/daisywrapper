package eu.nitrogensensor.daisy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    /**
     * Kloner en mappe rekursivt - en kopi af mappestrukturen laves, og fyldes med symbolske links til den oprindelige mappe
     *
     * @param fraMappe Mappen, der skal kopieres
     * @param tilMappe Destination. Indhold i mappen overskrives hvis det allerede findes
     */
    public static void klonMappe(Path fraMappe, Path tilMappe) throws IOException {
        final Path fra = fraMappe.toAbsolutePath(); // Fuld sti
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

}
