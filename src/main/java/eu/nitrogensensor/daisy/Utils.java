package eu.nitrogensensor.daisy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
    /**
     * Kloner en mappe rekursivt - en kopi af mappestrukturen laves, og fyldes med symbolske links til den oprindelige mappe
     *
     * @param fraMappe Mappen, der skal kopieres
     * @param tilMappe Destination. Indhold i mappen overskrives hvis det allerede findes
     */
    public static void klonMappe(Path fraMappe, Path tilMappe) {
        final Path fra = fraMappe.toAbsolutePath(); // Fuld sti
        try {
            Files.walk(fra).forEach(fraFil -> {
                try {
                    Path tilFil = tilMappe.resolve(fra.relativize(fraFil));
                    if( Files.isDirectory(fraFil)) {
                        if(!Files.exists(tilFil)) Files.createDirectory(tilFil);
                        return;
                    }
                    Files.deleteIfExists(tilFil);
                    //Files.copy( s, d );
                    Files.createSymbolicLink( tilFil, fraFil );
                } catch( Exception e ) {
                    e.printStackTrace();
                }
            });
        } catch( Exception ex ) {
            ex.printStackTrace();
        }
    }

}
