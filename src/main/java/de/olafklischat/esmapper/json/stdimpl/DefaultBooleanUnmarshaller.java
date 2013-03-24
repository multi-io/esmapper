package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultBooleanUnmarshaller implements JsonUnmarshaller {

    @Override
    public boolean readJson(JsonReader r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (r.peek() != JsonToken.BOOLEAN) {
            return false;
        }
        targetPath.set(r.nextBoolean());
        return true;
    }

}
