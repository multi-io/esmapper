package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;

import com.google.gson.JsonElement;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultNullUnmarshaller implements JsonUnmarshaller {

    @Override
    public boolean readJson(JsonElement r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (! r.isJsonNull()) {
            return false;
        }
        targetPath.set(null);
        return true;
    }

}
