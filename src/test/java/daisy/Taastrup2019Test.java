package daisy;


import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;


public class Taastrup2019Test {

    @Test
    public void timeCompilation() throws IOException {
        System.out.println(Arrays.asList(Paths.get("src/test/resources/Taastrup 2019/dtu_model").toFile().list()));
        assertEquals(1+1,2);
    }

}