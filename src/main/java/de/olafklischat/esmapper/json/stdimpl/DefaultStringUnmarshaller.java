package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultStringUnmarshaller implements JsonUnmarshaller {

    @Override
    public boolean readJson(JsonElement r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (! r.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive jsp = r.getAsJsonPrimitive();
        if (! jsp.isString()) {
            return false;
        }
        targetPath.set(jsp.getAsString());
        return true;
    }

}
