package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;
import java.lang.reflect.Array;

import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonMarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultArrayMarshaller implements JsonMarshaller {

    @Override
    public boolean writeJson(PropertyPath sourcePath, JsonWriter out, JsonConverter converter)
            throws IOException {
        Object src = sourcePath.get();
        if (!src.getClass().isArray()) {
            return false;
        }
        out.beginArray();
        int length = Array.getLength(src);
        for (int i = 0; i < length; i++) {
            PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(i, src), sourcePath);
            converter.writeJson(elementPath, out);
        }
        out.endArray();
        return true;
    }

}
