package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;
import java.util.Collection;

import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonMarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultCollectionMarshaller implements JsonMarshaller {

    @Override
    public boolean writeJson(PropertyPath sourcePath, JsonWriter out, JsonConverter converter)
            throws IOException {
        Object src = sourcePath.get();
        if (!(src instanceof Collection<?>)) {
            return false;
        }
        Collection<?> srcColl = (Collection<?>) src;
        out.beginArray();
        //TODO might want to remember the collection's class here
        //out.name("_collClass");
        //out.value(src.getClass().getCanonicalName());
        for (int i = 0; i < srcColl.size(); i++) {
            PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(i, src), sourcePath);
            if (converter.shouldMarshal(elementPath)) {
                converter.writeJson(elementPath, out);
            }
        }
        out.endArray();
        return true;
    }

}
