package de.olafklischat.esmapper.json.stdimpl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;

import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonMarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultBeanMarshaller implements JsonMarshaller {

    @Override
    public boolean writeJson(PropertyPath sourcePath, JsonWriter out, JsonConverter converter)
            throws IOException {
        Object src = sourcePath.get();
        out.beginObject();
        out.name("_class");
        out.value(src.getClass().getCanonicalName());
        try (JsonWriterEndObjectCloseWrapper outWrapper = new JsonWriterEndObjectCloseWrapper(out)) {
            BeanInfo bi = Introspector.getBeanInfo(src.getClass());
            for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
                if ("class".equals(pd.getName())) { //TODO: exclude anything from j.l.Object?
                    continue;
                }
                PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(pd, src), sourcePath);
                if (converter.shouldMarshal(elementPath)) {
                    out.name(pd.getName());
                    converter.writeJson(elementPath, out);
                }
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("error introspecting " + src + ": " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * JsonWriter wrapper to get Java7 ARM behaviour for {@link JsonWriter#endObject()}
     */
    private static class JsonWriterEndObjectCloseWrapper implements AutoCloseable {
        
        private final JsonWriter wr;
        
        public JsonWriterEndObjectCloseWrapper(JsonWriter wr) {
            this.wr = wr;
        }
        
        @Override
        public void close() throws Exception {
            wr.endObject();
        }
    }
    
}
