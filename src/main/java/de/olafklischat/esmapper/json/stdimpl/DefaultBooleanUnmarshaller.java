package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultBooleanUnmarshaller implements JsonUnmarshaller {

    @Override
    public boolean readJson(JsonElement r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (! r.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive jsp = r.getAsJsonPrimitive();
        if (! jsp.isBoolean()) {
            return false;
        }
        targetPath.set(jsp.getAsBoolean());
        return true;
    }

}
