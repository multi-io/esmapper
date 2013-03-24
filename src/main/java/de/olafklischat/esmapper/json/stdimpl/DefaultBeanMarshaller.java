package de.olafklischat.esmapper.json.stdimpl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;

import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.annotations.Ignore;
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
        try {
            BeanInfo bi = Introspector.getBeanInfo(src.getClass());
            for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
                if ("class".equals(pd.getName())) { //TODO: exclude anything from j.l.Object?
                    continue;
                }
                PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(pd, src), sourcePath);
                if (null != elementPath.getAnnotation(Ignore.class)) {
                    continue;
                }
                out.name(pd.getName());
                converter.writeJson(elementPath, out);
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("error introspecting " + src + ": " + e.getLocalizedMessage(), e);
        } finally {
            out.endObject();
        }
    }

}
