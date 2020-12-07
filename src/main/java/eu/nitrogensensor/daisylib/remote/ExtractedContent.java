package eu.nitrogensensor.daisylib.remote;

import java.io.IOException;
import java.util.HashMap;

public class ExtractedContent {
    public String id;
    public HashMap<String,String> fileContensMap = new HashMap<>();
    public Throwable exception;

    @Override
    public String toString() {
        return "ExtractedContent{" +
                "id='" + id + '\'' +
                ", fileContensMap=" + fileContensMap.keySet() +
                '}';
    }
}
