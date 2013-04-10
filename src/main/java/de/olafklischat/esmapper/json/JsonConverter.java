package de.olafklischat.esmapper.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.stdimpl.DefaultArrayMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultArrayUnmarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultBeanMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultBooleanMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultBooleanUnmarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultCollectionMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultMapMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultNullMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultNullUnmarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultNumberMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultNumberUnmarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultObjectUnmarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultStringMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultStringUnmarshaller;

public class JsonConverter {

    //implement our own object mapper/unmapper on top of Gson's low-level JSON streaming API
    // Gson's object mapper doesn't support everything we need
    
    private final Deque<JsonMarshaller> marshallers = new LinkedList<JsonMarshaller>();
    private final Deque<JsonUnmarshaller> unmarshallers = new LinkedList<JsonUnmarshaller>();
    
    private final Map<String, Object> attributes = new HashMap<String, Object>();
    
    public JsonConverter() {
        registerMarshaller(new DefaultBeanMarshaller());
        registerMarshaller(new DefaultMapMarshaller());
        registerMarshaller(new DefaultArrayMarshaller());
        registerMarshaller(new DefaultCollectionMarshaller());
        registerMarshaller(new DefaultStringMarshaller());
        registerMarshaller(new DefaultBooleanMarshaller());
        registerMarshaller(new DefaultNumberMarshaller());
        registerMarshaller(new DefaultNullMarshaller());
        registerUnmarshaller(new DefaultObjectUnmarshaller());
        registerUnmarshaller(new DefaultArrayUnmarshaller());
        registerUnmarshaller(new DefaultNumberUnmarshaller());
        registerUnmarshaller(new DefaultStringUnmarshaller());
        registerUnmarshaller(new DefaultBooleanUnmarshaller());
        registerUnmarshaller(new DefaultNullUnmarshaller());
    }

    
    public void registerMarshaller(JsonMarshaller m) {
        marshallers.addFirst(m);
    }
    
    public void registerUnmarshaller(JsonUnmarshaller um) {
        unmarshallers.addFirst(um);
    }

    /**
     * Put arbitrary attributes into this JsonConverter. Not used by
     * JsonConverter itself or any of the standard marshallers/unmarshallers.
     * But may be used by user code to pass information to custom
     * marshallers/unmarshallers.
     * 
     * @param name
     * @param value
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }
    
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

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
        jsw.setLenient(true);
        writeJson(src, jsw);
        jsw.close(); //not doing this in a finally because the writer may not be and end-of-document after
                     // an exception in a custom marshaller, in which case close() in a finally would
                     // throw another exception, which would shadow the original exception
                     // TODO: used a combined/grouped exception as in Java7?
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
        PropertyPath rootPath = new PropertyPath(new PropertyPath.Node((src == null ? Object.class : src.getClass())), null);
        rootPath.set(src);
        writeJson(rootPath, out);
    }
    
    public void writeJson(PropertyPath sourcePath, JsonWriter out) throws IOException {
        for (JsonMarshaller m : marshallers) {
            if (m.writeJson(sourcePath, out, this)) {
                break;
            }
        }
    }

    
    
    //// Deserialization (JSON->Object)

    @SuppressWarnings("unchecked")
    public <T> T fromJson(String json) {
        return (T) fromJson(json, Object.class);
    }
    
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return fromJson(new StringReader(json), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("JSON read error: " + e.getLocalizedMessage(), e);
        }
    }
    
    public void readJson(String json, Object target) {
        try {
            readJson(new StringReader(json), target);
        } catch (IOException e) {
            throw new IllegalStateException("JSON read error: " + e.getLocalizedMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(Reader r) throws IOException {
        return (T) fromJson(r, Object.class);
    }
    
    public <T> T fromJson(Reader r, Class<T> clazz) throws IOException {
        JsonReader jsr = new JsonReader(r);
        try {
            jsr.setLenient(true);
            return fromJson(jsr, clazz);
        } finally {
            jsr.close();
        }
    }
    
    public void readJson(Reader r, Object target) throws IOException {
        JsonReader jsr = new JsonReader(r);
        try {
            jsr.setLenient(true);
            readJson(Streams.parse(jsr), target);
        } finally {
            jsr.close();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonElement jse) throws IOException {
        return (T) fromJson(jse, Object.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonElement jse, Class<T> clazz) throws IOException {
        PropertyPath rootPath = new PropertyPath(new PropertyPath.Node(clazz), null);
        readJson(jse, rootPath);
        return (T) rootPath.get();
    }
    
    public void readJson(JsonElement jse, Object target) throws IOException {
        PropertyPath rootPath = new PropertyPath(new PropertyPath.Node(target.getClass()), null);
        rootPath.set(target);
        readJson(jse, rootPath);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonReader r) throws IOException {
        return (T) fromJson(r, Object.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonReader r, Class<T> clazz) throws IOException {
        PropertyPath rootPath = new PropertyPath(new PropertyPath.Node(clazz), null);
        JsonElement source = Streams.parse(r);
        readJson(source, rootPath);
        return (T) rootPath.get();
    }

    public void readJson(JsonElement source, PropertyPath targetPath) throws IOException {
        for (JsonUnmarshaller um : unmarshallers) {
            if (um.readJson(source, targetPath, this)) {
                break;
            }
        }
    }
    
}
