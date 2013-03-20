package de.olafklischat.esmapper.json;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class JsonConverter {

    //implement our own object mapper/unmapper on top of Gson's low-level JSON streaming API
    // Gson's object mapper doesn't support everything we need

    
    //// Serialization (Object->JSON)
    
    public String toJson(Object src) {
        StringWriter out = new StringWriter(300);
        try {
            writeJson(src, out);
            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException("BUG (shouldn't happen)", e);
        }
    }
    
    public void writeJson(Object src, Writer out) throws IOException {
        JsonWriter jsw = new JsonWriter(out);
        try {
            jsw.setLenient(true);
            writeJson(src, jsw);
        } finally {
            jsw.close();
        }
    }
    
    public JsonElement toJsonElement(Object src) {
        JsonTreeWriter jsw = new JsonTreeWriter();
        try {
            writeJson(src, jsw);
        } catch (IOException e) {
            throw new IllegalStateException("BUG (shouldn't happen)", e);
        }
        return jsw.get();
    }
    
    public void writeJson(Object src, JsonWriter out) throws IOException {
        if (src == null) {
            out.nullValue();
        } else {
            Class<?> c = src.getClass();
            if (c == Integer.TYPE || c == Long.TYPE || c == Double.TYPE || c == Float.TYPE || src instanceof Number) {
                out.value((Number)src);
            } else if (c == Boolean.TYPE || c == Boolean.class) {
                out.value((Boolean)src);
            } else if (src instanceof String) {
                out.value((String)src);
            } else if (src instanceof Collection<?>) {
                out.beginArray();
                //TODO might want to remember the collection's class here
                //out.name("_collClass");
                //out.value(src.getClass().getCanonicalName());
                for (Object elt : (Collection<?>) src) {
                    writeJson(elt, out);
                }
                out.endArray();
            } else if (c.isArray()) {
                out.beginArray();
                int length = Array.getLength(src);
                for (int i = 0; i < length; i++) {
                    writeJson(Array.get(src, i), out);
                }
                out.endArray();
            } else if (src instanceof Map<?, ?>) {
                Map<?, ?> srcMap = (Map<?, ?>) src;
                out.beginObject();
                out.name("_mapClass");
                out.value(srcMap.getClass().getCanonicalName());
                for (Object key : srcMap.keySet()) {
                    out.name(key.toString());
                    writeJson(srcMap.get(key), out);
                }
                out.endObject();
            } else {
                //assume src is a bean
                out.beginObject();
                out.name("_class");
                out.value(src.getClass().getCanonicalName());
                try {
                    BeanInfo bi = Introspector.getBeanInfo(src.getClass());
                    for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
                        if ("class".equals(pd.getName())) { //TODO: exclude anything from j.l.Object?
                            continue;
                        }
                        Method rm = pd.getReadMethod();
                        if (rm != null) {
                            out.name(pd.getName());
                            writeJson(rm.invoke(src), out);
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("error introspecting " + src + ": " + e.getLocalizedMessage(), e);
                } finally {
                    out.endObject();
                }
            }
        }
    }

    
    
    //// Deserialization (JSON->Object)

    public Object fromJson(String json) {
        try {
            return fromJson(new StringReader(json));
        } catch (IOException e) {
            throw new IllegalStateException("BUG (shouldn't happen)", e);
        }
    }
    
    
    public Object fromJson(Reader r) throws IOException {
        JsonReader jsr = new JsonReader(r);
        try {
            jsr.setLenient(true);
            return fromJson(jsr);
        } finally {
            jsr.close();
        }
    }
    
    public Object fromJson(JsonElement jse) throws IOException {
        try {
            JsonTreeReader jsr = new JsonTreeReader(jse);
            jsr.setLenient(true);
            return fromJson(jsr);
        } catch (IOException e) {
            throw new IllegalStateException("BUG (shouldn't happen)", e);
        }
    }

    public Object fromJson(JsonReader r) throws IOException {
        //TODO: remove or rewrite
        switch (r.peek()) {

        case NULL:
            r.nextNull();
            return null;

        case NUMBER:
            try {
                return r.nextInt();
            } catch (NumberFormatException e) {
                try {
                    return r.nextLong();
                } catch (NumberFormatException e2) {
                    return r.nextDouble();
                }
            }
            
        case BOOLEAN:
            return r.nextBoolean();
            
        case STRING:
            return r.nextString();
            
        default:
            throw new IllegalStateException("unexpected token: " + r.peek());

        }
    }
    
    public void readJson(JsonReader r, PropertyPath target) {
        
    }
    
}
