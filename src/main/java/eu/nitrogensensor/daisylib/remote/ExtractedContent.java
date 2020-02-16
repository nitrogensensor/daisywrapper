package eu.nitrogensensor.daisylib.remote;

import java.util.HashMap;

public class ExtractedContent {
    public String id;
    public HashMap<String,String> fileContensMap = new HashMap<>();

    @Override
    public String toString() {
        return "ExtractedContent{" +
                "id='" + id + '\'' +
                ", fileContensMap=" + fileContensMap.keySet() +
                '}';
    }
}
