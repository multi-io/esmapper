package de.olafklischat.esmapper.json.stdimpl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;

import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonMarshaller;
import de.olafklischat.esmapper.json.PropertyPath;
import de.olafklischat.esmapper.json.annotations.JsonIgnore;

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
                if (null != elementPath.getAnnotation(JsonIgnore.class)) {
                    continue;
                }
                out.name(pd.getName());
                converter.writeJson(elementPath, out);
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("error introspecting " + src + ": " + e.getLocalizedMessage(), e);
        } finally {
            //TODO if this throws, which it may because of an I/O error or (more likely)
            //  because writeJson above threw an exception and thus it's not legal to close the JSON object here,
            //  then that original exception will be shadowed by the endObject() exception. We'd need something
            //  like Java7's ARM/suppressedExceptions mechanism.
            out.endObject();
        }
    }

}
