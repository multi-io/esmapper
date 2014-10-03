package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;
import de.olafklischat.esmapper.json.annotations.JsonIgnore;
import de.olafklischat.esmapper.json.annotations.ImplClass;

public class DefaultObjectUnmarshaller implements JsonUnmarshaller {

    private static final Map<Class<?>, Class<?>> defaultMapImplClasses = new HashMap<Class<?>, Class<?>>();
    
    static {
        defaultMapImplClasses.put(BiMap.class, HashBiMap.class);
        defaultMapImplClasses.put(SortedMap.class, TreeMap.class);
        defaultMapImplClasses.put(Map.class, HashMap.class);
        defaultMapImplClasses.put(Object.class, HashMap.class);
    }
    
    @Override
    public boolean readJson(JsonElement r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (! r.isJsonObject()) {
            return false;
        }
        ////// 1. try to set() targetPath to an object that's an instance of targetPath's type (getNodeClass()),
        //////     *and* is a map or bean
        
        Object targetObject = null;
        try {
            targetObject = targetPath.get();
        } catch (Exception e) {
            //assume no error -- may be IndexOutOfBoundsExeption due to empty array/list etc.
        }
        
        JsonObject srcObj = r.getAsJsonObject();

        if (null == targetObject) {
            // targetPath hasn't been set() yet; we need to set it as described above.
            // This is the normal case. targetObject may only be non-null for root paths,
            // when the user called JsonConverter#readJson(JsonReader r, Object target)
            // to fill an existing object
    
            Class<?> targetClass = targetPath.getNodeClass();
    
            //try to instantiate targetObject from @ImplClass annotation, if present
            targetObject = tryCreateImplClassAnnotationInstance(targetPath, r);
    
            //if that didn't work, try to instantiate the class specified in _class or _mapClass in the JSON (if present)
            if (null == targetObject) {
                JsonElement className = srcObj.get("_class");
                if (null == className) {
                    className = srcObj.get("_mapClass");
                }
                if (null != className) {
                    try {
                        targetObject = Class.forName(className.getAsString()).newInstance();
                    } catch (Exception e) {
                        throw new IllegalStateException("" + targetPath +
                                ": couldn't instantiate " + className + ": " + e.getLocalizedMessage(), e);
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
        }

        ////// 2. targetObject has been set() into targetPath, now fill it
        for (Map.Entry<String, JsonElement> en : srcObj.entrySet()) {
            String key = en.getKey();
            if (key.startsWith("_")) { //skip any values whose keys start with "_"
                continue;
            }
            JsonElement value = en.getValue();
            PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(key, targetObject), targetPath);
            if (elementPath.getAnnotation(JsonIgnore.class) == null) {
                converter.readJson(value, elementPath);
            }
        }
        return true;
    }


    private Object tryCreateImplClassAnnotationInstance(PropertyPath target, JsonElement r) throws IOException {
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
