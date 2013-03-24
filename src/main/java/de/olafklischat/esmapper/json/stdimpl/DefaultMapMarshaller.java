package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;
import java.util.Map;

import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonMarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultMapMarshaller implements JsonMarshaller {

    @Override
    public boolean writeJson(PropertyPath sourcePath, JsonWriter out, JsonConverter converter)
            throws IOException {
        Object src = sourcePath.get();
        if (!(src instanceof Map<?, ?>)) {
            return false;
        }
        Map<?, ?> srcMap = (Map<?, ?>) src;
        out.beginObject();
        out.name("_mapClass");
        out.value(srcMap.getClass().getCanonicalName());
        for (Object key : srcMap.keySet()) {
            out.name(key.toString());
            PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(key.toString(), src), sourcePath);
            converter.writeJson(elementPath, out);
        }
        out.endObject();
        return true;
    }

}
