package de.olafklischat.esmapper.json;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * A path to a property in an object graph, i.e. something like
 * object.someProp.someArr[5].someMap["hello"].otherProp
 * 
 * @author olaf
 */
public class PropertyPath {

    /**
     * A single node of a property path, i.e. a property accessor, array index
     * accessor, or map key accessor.
     * 
     * @author olaf
     * 
     */
    public static class Node {
        public static enum Type { PROPERTY, ARRAY_ELEMENT, MAP_VALUE }

        private Type type;
        private PropertyDescriptor propDescriptor;
        private int arrayIndex;
        private String mapKey;
        
        private Object target;
        
        public Type getType() {
            return type;
        }

        /**
         * the object on which to apply the node. Must be a map if
         * type==MAP_VALUE, an array or collection if type==ARRAY_ELEMENT, or an
         * arbitrary bean if type==PROPERTY.
         * 
         * TODO: allow null value? ("uninstantiated" paths)
         * 
         * @return
         */
        public Object getTarget() {
            return target;
        }
        
        public PropertyDescriptor getPropDescriptor() {
            if (type != Type.PROPERTY) {
                throw new IllegalStateException();
            }
            return propDescriptor;
        }
        
        public int getArrayIndex() {
            if (type != Type.ARRAY_ELEMENT) {
                throw new IllegalStateException();
            }
            return arrayIndex;
        }
        
        public String getMapKey() {
            if (type != Type.MAP_VALUE) {
                throw new IllegalStateException();
            }
            return mapKey;
        }
        
        public Class<?> getNodeClass() {
            switch (type) {

            case PROPERTY:
                return propDescriptor.getPropertyType();
            
            case ARRAY_ELEMENT:
                throw new IllegalStateException("NYI");
            
            case MAP_VALUE:
                return Object.class; //TODO: return the map's generics value type, if defined
            
            default:
                throw new IllegalStateException("shouldn't happen");
            }
        }
        
        public Object get() {
            switch (type) {

            case PROPERTY:
                Method rm = propDescriptor.getReadMethod();
                if (rm != null) {
                    try {
                        return rm.invoke(target);
                    } catch (Exception e) {
                        throw new IllegalStateException("error reading property " + this + ": " +
                                e.getLocalizedMessage(), e);
                    }
                } else {
                    throw new IllegalStateException("property not readable: " + this);
                }

            case ARRAY_ELEMENT:
                throw new IllegalStateException("NYI");

            case MAP_VALUE:
                throw new IllegalStateException("NYI");
            
            default:
                throw new IllegalStateException("shouldn't happen");
            }
        }
        
        public void set(Object value) {
            switch (type) {

            case PROPERTY:
                Method wm = propDescriptor.getWriteMethod();
                if (wm != null) {
                    try {
                        wm.invoke(target, value);
                    } catch (Exception e) {
                        throw new IllegalStateException("error setting property " + this +
                                " to " + value + ": " + e.getLocalizedMessage(), e);
                    }
                } else {
                    throw new IllegalStateException("property not writable: " + this);
                }

            case ARRAY_ELEMENT:
                throw new IllegalStateException("NYI");

            case MAP_VALUE:
                throw new IllegalStateException("NYI");
            
            default:
                throw new IllegalStateException("shouldn't happen");
            }
        }

    }
    
    private List<Node> nodes;
    
    
}
