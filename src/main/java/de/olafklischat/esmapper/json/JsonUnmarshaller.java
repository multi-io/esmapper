package de.olafklischat.esmapper.json;

import java.io.IOException;

import com.google.gson.JsonElement;

public interface JsonUnmarshaller {
    boolean readJson(JsonElement source, PropertyPath targetPath, JsonConverter context) throws IOException;
}
