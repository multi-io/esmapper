package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultStringUnmarshaller implements JsonUnmarshaller {

    @Override
    public boolean readJson(JsonReader r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (r.peek() != JsonToken.STRING) {
            return false;
        }
        targetPath.set(r.nextString());
        return true;
    }

}
