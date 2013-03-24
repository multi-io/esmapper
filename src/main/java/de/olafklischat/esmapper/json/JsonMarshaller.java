package de.olafklischat.esmapper.json;

import java.io.IOException;

import com.google.gson.stream.JsonWriter;

public interface JsonMarshaller {
    boolean writeJson(PropertyPath sourcePath, JsonWriter out, JsonConverter context) throws IOException;
}
