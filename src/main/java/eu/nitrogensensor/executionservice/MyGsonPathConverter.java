package eu.nitrogensensor.executionservice;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MyGsonPathConverter implements JsonDeserializer<Path>, JsonSerializer<Path> {

    public static Gson buildGson() {
        Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(Path.class, new MyGsonPathConverter()).create();
        return gson;
    }

    @Override
    public Path deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return Paths.get(jsonElement.getAsString());
    }

    @Override
    public JsonElement serialize(Path path, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(path.toString());
    }
}