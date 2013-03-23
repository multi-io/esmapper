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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.util.HashSet;

import de.olafklischat.esmapper.annotations.Ignore;
import de.olafklischat.esmapper.annotations.ImplClass;

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
    
    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonElement jse) throws IOException {
        return (T) fromJson(jse, Object.class);
    }

    public <T> T fromJson(JsonElement jse, Class<T> clazz) throws IOException {
        try {
            JsonTreeReader jsr = new JsonTreeReader(jse);
            jsr.setLenient(true);
            return fromJson(jsr, clazz);
        } catch (IOException e) {
            throw new IllegalStateException("BUG (shouldn't happen)", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonReader r) throws IOException {
        return (T) fromJson(r, Object.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonReader r, Class<T> clazz) throws IOException {
        PropertyPath rootPath = new PropertyPath(new PropertyPath.Node(clazz), null);
        readJson(r, rootPath);
        return (T) rootPath.get();
    }
    
    public void readJson(JsonReader r, PropertyPath targetPath) throws IOException {
        switch (r.peek()) {

        case STRING:
            targetPath.set(r.nextString());
            break;
            
        case BOOLEAN:
            targetPath.set(r.nextBoolean());
            break;
            
        case NUMBER:
            Number fromJson;
            try {
                fromJson = r.nextInt();
            } catch (NumberFormatException e) {
                try {
                    fromJson = r.nextLong();
                } catch (NumberFormatException e2) {
                    fromJson = r.nextDouble();
                }
            }
            targetPath.set(convertNumber(fromJson, targetPath.getNodeClass()));
            break;
            
        case NULL:
            r.nextNull();
            targetPath.set(null);
            break;

        case BEGIN_ARRAY: {
            ////// 1. try to set() targetPath to an object that's an instance of targetPath's type (getNodeClass()),
            //////     *and* is a collection or array
            
            Class<?> targetClass = targetPath.getNodeClass();
            ////TODO array properties are a bit nasty because we have to know the length in advance.
            //    (or re-allocate the array every time in ProertyPath.Node#set)
            if (targetClass.isArray()) {
                throw new IllegalStateException("um... array properties not supported yet :-P (" + targetPath + ")");
            }

            Object targetObject = null;
            
            ////try to instantiate targetObject from @ImplClass annotation, if present
            if (null != (targetObject = tryCreateImplClassAnnotationInstance(targetPath, r))) {
                if (!(targetObject instanceof Collection<?> || targetObject.getClass().isArray())) {
                    throw new IllegalArgumentException("" + targetPath + ": JSON contains an array, but target type isn't compatible to that");
                }
            }
            
            ////otherwise, check if targetClass is some well-known interface for which we know a good implementation
            if (null == targetObject) {
                for (Class<?> declaredClass: defaultCollectionImplClasses.keySet()) {
                    if (targetClass.equals(declaredClass)) {
                        Class<?> implClass = defaultCollectionImplClasses.get(declaredClass);
                        try {
                            targetObject = implClass.newInstance();
                            break;
                        } catch (Exception e) {
                            throw new IllegalArgumentException("shouldn't happen", e);
                        }
                    }
                }
            }

            ////...if that didn't work either, try to instantiate the property class directly as a last resort
            if (null == targetObject) {
                if (!(Collection.class.isAssignableFrom(targetClass))) {
                    throw new IllegalArgumentException("" + targetPath + ": JSON contains an array, but target type isn't compatible to that");
                }
                try {
                    targetObject = targetClass.newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("" + targetPath + ": couldn't instantiate property directly: " +
                                e.getLocalizedMessage(), e);
                }
            }
            
            ////...if that didn't work either, give up
            if (null == targetObject) {
                throw new IllegalArgumentException("failed to instantiate property " + targetPath);
            }

            //targetObject has been created successfully
            targetPath.set(targetObject);
            
            ////// 2. fill the created target object with the array elements
            r.beginArray();
            int index = 0;
            while (r.hasNext()) {
                PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(index, targetObject), targetPath);
                readJson(r, elementPath);
                index++;
            }
            r.endArray();
        }
        break;
            
        case BEGIN_OBJECT: {
            ////// 1. try to set() targetPath to an object that's an instance of targetPath's type (getNodeClass()),
            //////     *and* is a map or bean
            
            Object targetObject = null;

            ///we'll read the first key into this variable during targetObject instantiation,
            //  before looping over all the map elements in the JSON. Reason: we need to
            //  check for _class / _mapClass keys. If those aren't present, firstKey will
            //  contain the name of the first regular property / map key to set
            String firstKey = null;

            Class<?> targetClass = targetPath.getNodeClass();

            //try to instantiate targetObject from @ImplClass annotation, if present
            if (null != (targetObject = tryCreateImplClassAnnotationInstance(targetPath, r))) {
                r.beginObject(); // skip over the { because we do it in the next alternative (below) too
            }

            //if that didn't work, try to instantiate the class specified in _class or _mapClass in the JSON (if present)
            if (null == targetObject) {
                //need to fetch the first key of the map to see if it is _class or _mapClass,
                // in which case targetObject must become an instance of that.
                r.beginObject();
                if (r.hasNext()) {
                    firstKey = r.nextName();
                    if ("_class".equals(firstKey) || "_mapClass".equals(firstKey)) {
                        if (r.peek() != JsonToken.STRING) {
                            throw new IllegalStateException("" + targetPath + ": _class/_mapClass require a string value");
                        }
                        String className = r.nextString();
                        firstKey = null;
                        try {
                            targetObject = Class.forName(className).newInstance();
                        } catch (Exception e) {
                            throw new IllegalStateException("" + targetPath +
                                    ": couldn't instantiate " + className + ": " + e.getLocalizedMessage(), e);
                        }
                    }
                }
            }

            //...if that didn't work, check if targetClass is some well-known interface for which we know a good implementation
            if (null == targetObject) {
                for (Class<?> declaredClass: defaultMapImplClasses.keySet()) {
                    if (targetClass.equals(declaredClass)) {
                        Class<?> implClass = defaultMapImplClasses.get(declaredClass);
                        try {
                            targetObject = implClass.newInstance();
                            break;
                        } catch (Exception e) {
                            throw new IllegalArgumentException("shouldn't happen", e);
                        }
                    }
                }
            }
            
            //...if that didn't work either, try to instantiate the property class directly
            if (null == targetObject) {
                try {
                    targetObject = targetClass.newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("" + targetPath + ": couldn't instantiate property directly: " +
                                e.getLocalizedMessage(), e);
                }
            }
            
            //...if that didn't work either, give up
            if (null == targetObject) {
                throw new IllegalArgumentException("failed to instantiate property " + targetPath);
            }

            //targetObject has been created successfully
            targetPath.set(targetObject);

            ////// 2. targetObject has been set() into targetPath, now fill it
            if (firstKey != null) {
                //firstKey is a regular property / map key, and r is located at the beginning of the value
                if (firstKey.startsWith("_")) { //skip any values whose keys start with "_"
                    r.skipValue();
                } else {
                    //else, read the value and set it into targetObject
                    PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(firstKey, targetObject), targetPath);
                    if (elementPath.getAnnotation(Ignore.class) == null) {
                        readJson(r, elementPath);
                    } else {
                        r.skipValue();
                    }
                }
            }
            while (r.hasNext()) {
                String key = r.nextName();
                if (key.startsWith("_")) { //skip any values whose keys start with "_"
                    r.skipValue();
                } else {
                    //else, read the value and set it into targetObject
                    PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(key, targetObject), targetPath);
                    if (elementPath.getAnnotation(Ignore.class) == null) {
                        readJson(r, elementPath);
                    } else {
                        r.skipValue();
                    }
                }
            }
            r.endObject();
        }
        break;
            
        default:
            throw new IllegalStateException("unexpected token: " + r.peek());

        }
    }
    
    private Object convertNumber(Number n, Class<?> targetType) {
        if (targetType.isPrimitive()) {
            //in this case, the JVM does the conversion
            return n;
        } else if (targetType == Long.class && (n instanceof Integer || n instanceof Long)) {
            return n.longValue();
        } else if (targetType == Float.class) {
            return n.floatValue();
        } else if (targetType == Double.class) {
            return n.doubleValue();
        } else {
            return n;
        }
    }

    private Object tryCreateImplClassAnnotationInstance(PropertyPath target, JsonReader r) throws IOException {
        ImplClass ic = target.getAnnotation(ImplClass.class);
        if (null != ic) {
            try {
                return ic.value().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("" + target + ": Failed to instantiate @ImplClass ("
                            + ic.value().getCanonicalName() + "): " + e.getLocalizedMessage(), e);
            }
        }
        return null;
    }
    
    private static final Map<Class<?>, Class<?>> defaultCollectionImplClasses = new HashMap<Class<?>, Class<?>>();
    private static final Map<Class<?>, Class<?>> defaultMapImplClasses = new HashMap<Class<?>, Class<?>>();
    
    static {
        defaultCollectionImplClasses.put(Collection.class, ArrayList.class);
        defaultCollectionImplClasses.put(List.class, ArrayList.class);
        defaultCollectionImplClasses.put(Set.class, HashSet.class);
        defaultCollectionImplClasses.put(Object.class, ArrayList.class);

        defaultMapImplClasses.put(BiMap.class, HashBiMap.class);
        defaultMapImplClasses.put(SortedMap.class, TreeMap.class);
        defaultMapImplClasses.put(Map.class, HashMap.class);
        defaultMapImplClasses.put(Object.class, HashMap.class);
    }
    
}
