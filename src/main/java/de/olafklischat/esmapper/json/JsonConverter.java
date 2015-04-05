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

import de.olafklischat.esmapper.json.annotations.JsonIgnore;
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
import de.olafklischat.esmapper.json.stdimpl.JsonIgnoreMarshallingFilter;

/**
 * Central external API entry point for marshalling and unmarshalling JSON.
 * The toJson() / writeJson() methods are used for marshalling (Java->JSON),
 * the fromJson() / readJson() methods are used for unmarshalling (JSON->Java).
 * <p>
 * The actual marshalling and unmarshalling work is done by {@link JsonMarshaller marshallers}
 * and {@link JsonUnmarshaller unmarshallers}, which are registered with the JsonConverter
 * via the register* methods. They are invoked for every subtree of the input JSON or Java object graph
 * as it needs to be written to the output Java object or JSON stream. JsonConverter
 * itself already registers default marshallers and unmarshallers to support all the
 * common kinds of subtrees, e.g. primitives, objects, arrays/lists, and maps.
 * <p>
 * JsonConverter objects are stateless except for the registered marshallers/unmarshallers
 * and the {@link #setAttribute(String, Object) attributes} (which aren't used
 * by JsonConverter itself or the standard marshallers/unmarshallers). This means
 * that you can use the same JsonConverter instance from multiple threads as long
 * as you take care not to modify the marshaller/unmarshaller list or the attributes
 * concurrently.
 * 
 * @author Olaf Klischat
 *
 */
public class JsonConverter {

    //implement our own object mapper/unmapper on top of Gson's low-level JSON streaming API
    // Gson's object mapper doesn't support everything we need

    private final Deque<JsonMarshallingFilter> marshallingFilters = new LinkedList<JsonMarshallingFilter>();
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
        registerMarshallingFilter(new JsonIgnoreMarshallingFilter());
        registerUnmarshaller(new DefaultObjectUnmarshaller());
        registerUnmarshaller(new DefaultArrayUnmarshaller());
        registerUnmarshaller(new DefaultNumberUnmarshaller());
        registerUnmarshaller(new DefaultStringUnmarshaller());
        registerUnmarshaller(new DefaultBooleanUnmarshaller());
        registerUnmarshaller(new DefaultNullUnmarshaller());
    }

    /**
     * Register a {@link JsonMarshallingFilter} with this JsonConverter for
     * use during marshalling (Java->JSON) operations.
     * <p>
     * JsonConverter itself registers one such filter: {@link JsonIgnoreMarshallingFilter},
     * which filters out properties annotated with the {@link JsonIgnore} annotation.
     * 
     * @param filter
     */
    public void registerMarshallingFilter(JsonMarshallingFilter filter) {
        marshallingFilters.add(filter);
    }
    
    /**
     * Register a {@link JsonMarshaller marshaller} for use with this JsonConverter.
     * Marshallers perform all the actual marshalling work; they're called
     * whenever a part (subtree) of the input object must be written.
     * <p>
     * JsonConverter itself already registers all the default marshallers (see
     * the stdimpl subpackage) needed for marshalling all supported kinds
     * of Java object subtrees (primitives, objects, arrays/collections and maps).
     * 
     * @param m
     */
    public void registerMarshaller(JsonMarshaller m) {
        marshallers.addFirst(m);
    }
    
    /**
     * Register a {@link JsonUnmarshaller unmarshaller} for use with this JsonConverter.
     * Unmarshallers perform all the actual unmarshalling work; they're called
     * whenever a part (subtree) of the input JSON must be written to a subtree of the
     * destination Java object graph.
     * <p>
     * JsonConverter itself already registers all the default unmarshallers (see
     * the stdimpl subpackage) needed for marshalling all supported kinds
     * of JSON subtrees (primitives, objects, arrays and maps).
     * 
     * @param m
     */
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
        try (JsonWriter jsw = new JsonWriter(out)) {
            jsw.setLenient(true);
            writeJson(src, jsw);
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
        PropertyPath rootPath = new PropertyPath(new PropertyPath.Node((src == null ? Object.class : src.getClass())), null);
        rootPath.set(src);
        writeJson(rootPath, out);
    }

    /**
     * Write (marshal) sourcePath and all its referenced objects into out. Usually
     * not called directly by outside parties, but only by marshallers and the other 
     * write* / toJson() methods.
     * <p>
     * All write* / toJson() methods delegate to this one. This method calls all
     * {@link #registerMarshaller(JsonMarshaller) registered marshallers} (in reverse
     * order of registering) until one returns true, indicating that it wrote the subtree
     * to the output.
     * 
     * @param sourcePath
     * @param out
     * @throws IOException
     */
    public void writeJson(PropertyPath sourcePath, JsonWriter out) throws IOException {
        for (JsonMarshaller m : marshallers) {
            if (m.writeJson(sourcePath, out, this)) {
                break;
            }
        }
    }

    /**
     * Tell whether sourcePath should be marshalled, by checking against
     * the registered JsonMarshallingFilters (see {@link #registerMarshallingFilter(JsonMarshallingFilter)}).
     * Returns false if and only if any of those filters returns false.
     * <p>
     * Meant to be called by {@link JsonMarshaller marshallers} during a marshalling (Java->JSON) operation.
     * For consistent and predictable behavior, all marshallers that have subtrees in their input that they want
     * to pass to {@link JsonConverter#writeJson(PropertyPath, JsonWriter)} should first determine
     * whether or not to do that by calling this method.
     * 
     * @param sourcePath
     * @return
     */
    public boolean shouldMarshal(PropertyPath sourcePath) {
        for (JsonMarshallingFilter f : marshallingFilters) {
            if (!f.shouldMarshal(sourcePath, this)) {
                return false;
            }
        }
        return true;
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

    /**
     * Read (unmarshal) source and its subtree into targetPath. Usually
     * not called directly by outside parties, but only by unmarshallers and the other 
     * read* / fromJson() methods.
     * <p>
     * All read* / fromJson() methods delegate to this one. This method calls all
     * {@link #registerUnmarshaller(JsonUnmarshaller) registered unmarshallers} (in reverse
     * order of registering) until one returns true, indicating that it read the subtree
     * into targetPath.
     * 
     * @param source
     * @param targetPath
     * @throws IOException
     */
    public void readJson(JsonElement source, PropertyPath targetPath) throws IOException {
        for (JsonUnmarshaller um : unmarshallers) {
            if (um.readJson(source, targetPath, this)) {
                break;
            }
        }
    }
    
}
