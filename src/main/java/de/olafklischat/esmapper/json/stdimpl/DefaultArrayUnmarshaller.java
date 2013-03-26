package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.util.HashSet;

import de.olafklischat.esmapper.annotations.ImplClass;
import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultArrayUnmarshaller implements JsonUnmarshaller {

    private static final Map<Class<?>, Class<?>> defaultCollectionImplClasses = new HashMap<Class<?>, Class<?>>();
    
    static {
        defaultCollectionImplClasses.put(Collection.class, ArrayList.class);
        defaultCollectionImplClasses.put(List.class, ArrayList.class);
        defaultCollectionImplClasses.put(Set.class, HashSet.class);
        defaultCollectionImplClasses.put(Object.class, ArrayList.class);
    }

    @Override
    public boolean readJson(JsonReader r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (r.peek() != JsonToken.BEGIN_ARRAY) {
            return false;
        }
        ////// 1. try to set() targetPath to an object that's an instance of targetPath's type (getNodeClass()),
        //////     *and* is a collection or array
        
        Object targetObject = null;
        try {
            targetObject = targetPath.get();
        } catch (Exception e) {
            //assume no error -- may be IndexOutOfBoundsExeption due to empty array/list etc.
        }
        
        if (null == targetObject) {
            // targetPath hasn't been set() yet; we need to set it as described above.
            // This is the normal case. targetObject may only be non-null for root paths,
            // when the user called JsonConverter#readJson(JsonReader r, Object target)
            // to fill an existing array/list
            
            targetObject = createInstance(r, targetPath, converter);
            
            //targetObject has been created successfully
            targetPath.set(targetObject);
        }
        
        ////// 2. fill the created target object with the array elements
        r.beginArray();
        int index = 0;
        while (r.hasNext()) {
            PropertyPath elementPath = new PropertyPath(new PropertyPath.Node(index, targetObject), targetPath);
            converter.readJson(r, elementPath);
            index++;
        }
        r.endArray();
        return true;
    }
    
    
    protected Object createInstance(JsonReader r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {

        Class<?> targetClass = targetPath.getNodeClass();
        ////TODO array properties are a bit nasty because we have to know the length in advance.
        //    (or re-allocate the array every time in ProertyPath.Node#set)
        if (targetClass.isArray()) {
            throw new IllegalStateException("um... array properties not supported yet :-P (" + targetPath + ")");
        }

        Object targetObject;

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
        
        return targetObject;
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
