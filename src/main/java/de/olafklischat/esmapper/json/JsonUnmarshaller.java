package de.olafklischat.esmapper.json;

import java.io.IOException;

import com.google.gson.stream.JsonReader;

public interface JsonUnmarshaller {
    boolean readJson(JsonReader r, PropertyPath targetPath, JsonConverter context) throws IOException;
}
