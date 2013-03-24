package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import de.olafklischat.esmapper.annotations.Ignore;
import de.olafklischat.esmapper.annotations.ImplClass;
import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultObjectUnmarshaller implements JsonUnmarshaller {

    private static final Map<Class<?>, Class<?>> defaultMapImplClasses = new HashMap<Class<?>, Class<?>>();
    
    static {
        defaultMapImplClasses.put(BiMap.class, HashBiMap.class);
        defaultMapImplClasses.put(SortedMap.class, TreeMap.class);
        defaultMapImplClasses.put(Map.class, HashMap.class);
        defaultMapImplClasses.put(Object.class, HashMap.class);
    }
    
    @Override
    public boolean readJson(JsonReader r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (r.peek() != JsonToken.BEGIN_OBJECT) {
            return false;
        }
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
                    converter.readJson(r, elementPath);
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
                    converter.readJson(r, elementPath);
                } else {
                    r.skipValue();
                }
            }
        }
        r.endObject();
        return true;
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
    
}
