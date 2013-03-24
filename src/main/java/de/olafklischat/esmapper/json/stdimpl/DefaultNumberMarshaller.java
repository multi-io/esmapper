package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;

import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonMarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultNumberMarshaller implements JsonMarshaller {

    @Override
    public boolean writeJson(PropertyPath sourcePath, JsonWriter out, JsonConverter converter)
            throws IOException {
        Object src = sourcePath.get();
        Class<?> c = src.getClass();
        if (c == Integer.TYPE || c == Long.TYPE || c == Double.TYPE || c == Float.TYPE || src instanceof Number) {
            out.value((Number)src);
            return true;
        } else {
            return false;
        }

    }

}
