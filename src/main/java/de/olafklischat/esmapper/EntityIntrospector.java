package de.olafklischat.esmapper;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Id;
import javax.persistence.Version;

import de.olafklischat.esmapper.annotations.LoadedFlag;

/**
 * Introspector class that may be used to learn about persistence-related
 * properties of entity objects (id, version etc.).
 * <p>
 * Primarily meant for internal use, but may also be called from
 * outside/business code.
 * 
 * @author olaf
 */
public class EntityIntrospector {
    
    //TODO: consider size restriction (LRU)
    private static final Map<Class<?>, Accessors> accessorsCache = new HashMap<Class<?>, Accessors>();

    private static class Accessors {
        public final Accessor idAccessor, versionAccessor, loadedAccessor;

        public Accessors(Class<?> cl) {
            idAccessor = getAccessor(cl, Id.class, "id");
            if (null == idAccessor || idAccessor.getType() != String.class || ! idAccessor.isReadable() || ! idAccessor.isWritable()) {
                throw new IllegalStateException("entity " + cl + ": r/w string id property required");
            }
            versionAccessor = getAccessor(cl, Version.class, "version");
            if (null != versionAccessor && ! (Long.class.isAssignableFrom(versionAccessor.getType()) && versionAccessor.isReadable() && versionAccessor.isWritable())) {
                throw new IllegalStateException("entity " + cl + ": version property present, but not long and r/w");
            }
            loadedAccessor = getAccessor(cl, LoadedFlag.class, "loaded");
            if (null != loadedAccessor && ! (Boolean.TYPE == loadedAccessor.getType() && loadedAccessor.isReadable() && loadedAccessor.isWritable())) {
                throw new IllegalStateException("entity " + cl + ": loaded property present, but not boolean and r/w");
            }
        }
        
        private Accessor getAccessor(Class<?> cl, Class<? extends Annotation> ann, String name) {
            try {
                BeanInfo bi = Introspector.getBeanInfo(cl);
                PropertyDescriptor nameCandidate = null;
                for (PropertyDescriptor pd: bi.getPropertyDescriptors()) {
                    if (null != pd.getReadMethod().getAnnotation(ann)) {
                        return new Accessor(pd);
                    } else if (name.equals(pd.getName())) {
                        nameCandidate = pd;
                    }
                }
                if (null != nameCandidate) {
                    return new Accessor(nameCandidate);
                } else {
                    return null;
                }
            } catch (Exception e) {
                throw new IllegalStateException("error introspecting " + cl + ": " + e.getLocalizedMessage(), e);
            }
        }
    }
    
    private static class Accessor {
        private final PropertyDescriptor pd;
        public Accessor(PropertyDescriptor pd) {
            this.pd = pd;
        }
        public boolean isReadable() {
            return null != pd.getReadMethod();
        }
        public boolean isWritable() {
            return null != pd.getWriteMethod();
        }
        public Class<?> getType() {
            return pd.getPropertyType();
        }
        public Object get(Object entity) {
            try {
                return pd.getReadMethod().invoke(entity);
            } catch (Exception e) {
                throw new IllegalStateException("error getting " + pd.getName() + " of entity " + entity + ": " + e.getLocalizedMessage(), e);
            }
        }
        public void set(Object entity, Object value) {
            try {
                pd.getWriteMethod().invoke(entity, value);
            } catch (Exception e) {
                throw new IllegalStateException("error setting " + pd.getName() + " of entity " + entity + " to " + value + ": " + e.getLocalizedMessage(), e);
            }
        }
    }
    
    private static Accessors getAccessors(Object entity) {
        Class<?> cl = entity.getClass();
        synchronized (accessorsCache) {
            Accessors ms = accessorsCache.get(cl);
            if (ms == null) {
                ms = new Accessors(cl);
                accessorsCache.put(cl, ms);
            }
            return ms;
        }
    }

    /**
     * ID of the entity in the database. null for newly created entities
     * (which don't have a database identity yet).
     * => elasticsearch _id
     */
    public static String getId(Object entity) {
        return (String) getAccessors(entity).idAccessor.get(entity);
    }
    
    /**
     * Business code shouldn't call this.
     * 
     * @param isLoaded
     */
    public static void setId(Object entity, String id) {
        getAccessors(entity).idAccessor.set(entity, id);
    }

    public static boolean supportsVersion(Object entity) {
        return null != getAccessors(entity).versionAccessor;
    }

    /**
     * version of the entity in the database. Starts at one when storing a new
     * entity for the first time, incremented by one on each successive store
     * operation.
     * => elasticsearch _version
     */
    public static Long getVersion(Object entity) {
        return (Long) getAccessors(entity).versionAccessor.get(entity);
    }

    public static void setVersion(Object entity, Long version) {
        getAccessors(entity).versionAccessor.set(entity, version);
    }
    
    public static boolean supportsLoadedFlag(Object entity) {
        return null != getAccessors(entity).loadedAccessor;
    }

    public static boolean isLoaded(Object entity) {
        return (Boolean) getAccessors(entity).loadedAccessor.get(entity);
    }
    
    /**
     * Business code shouldn't call this.
     * 
     * @param isLoaded
     */
    public static void setLoaded(Object entity, boolean isLoaded) {
        getAccessors(entity).loadedAccessor.set(entity, isLoaded);
    }
    
}
