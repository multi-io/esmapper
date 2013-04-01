package de.olafklischat.esmapper.json;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.olafklischat.esmapper.json.PropertyPath.Node.Type;

/**
 * A path to a property in an object graph, i.e. something like
 * object.someProp.someArr[5].someMap["hello"].otherProp
 * 
 * @author olaf
 */
public class PropertyPath {
    
    private Node head;
    private PropertyPath tail;
    
    public PropertyPath(Node head, PropertyPath tail) {
        super();
        this.head = head;
        this.tail = tail;
        head.setPathToMe(this);
    }
    
    public Node getHead() {
        return head;
    }
    
    public PropertyPath getTail() {
        return tail;
    }
    
    public Type getType() {
        return head.getType();
    }

    public Object getBaseObject() {
        return head.getBaseObject();
    }

    public PropertyDescriptor getPropDescriptor() {
        return head.getPropDescriptor();
    }

    public int getArrayIndex() {
        return head.getArrayIndex();
    }

    public String getMapKey() {
        return head.getMapKey();
    }

    public Class<?> getNodeClass() {
        return head.getNodeClass();
    }
    
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return head.getAnnotation(annotationClass);
    }

    public Annotation[] getAnnotations() {
        return head.getAnnotations();
    }

    public Object get() {
        return head.get();
    }

    public void set(Object value) {
        head.set(value);
    }
    
    public int getLength() {
        if (tail == null) {
            return 1;
        } else {
            return 1 + tail.getLength();
        }
    }
    
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        if (tail != null) {
            result.append(tail.toString());
        }
        result.append(head.toString());
        return result.toString();
    }


    /**
     * A single node of a property path, i.e. a property accessor, array element
     * accessor, or map element accessor.
     * 
     * @author olaf
     * 
     */
    public static class Node {
        public static enum Type { PROPERTY, ARRAY_ELEMENT, MAP_VALUE, ROOT }

        private final Type type;
        
        //field is final, but can't be because compiler complains in Node(String,Object) c'tor
        private /*final*/ PropertyDescriptor propDescriptor; //needed when type==PROPERTY
        private final int arrayIndex;   //needed when type==ARRAY_ELEMENT
        private final String mapKey;    //needed when type==MAP_VALUE

        
        private final Object baseObject;
        
        private final Class<?> rootClass;  //needed when type==ROOT
        private Object rootObject;   //needed when type==ROOT
        // (ROOT is a special type used only for the topmost (root) object in JsonConverter.fromJson(json, root) et al.)
        
        private PropertyPath pathToMe;
        
        public Node(PropertyDescriptor propDescriptor, Object baseObject) {
            this(Type.PROPERTY, propDescriptor, 0, null, baseObject);
        }

        public Node(int arrayIndex, Object baseObject) {
            this(Type.ARRAY_ELEMENT, null, arrayIndex, null, baseObject);
        }
        
        public Node(Class<?> rootClass) {
            this.type = Type.ROOT;
            this.propDescriptor = null;
            this.arrayIndex = 0;
            this.mapKey = null;
            this.baseObject = null;
            this.rootClass = rootClass;
            this.pathToMe = new PropertyPath(this, null);
        }
        
        public Node(String mapKeyOrPropertyName, Object baseObject) {
            if (baseObject instanceof Map<?, ?>) {
                this.type = Type.MAP_VALUE;
                this.propDescriptor = null;
                this.arrayIndex = 0;
                this.mapKey = mapKeyOrPropertyName;
            } else {
                this.type = Type.PROPERTY;
                PropertyDescriptor[] pds;
                try {
                    pds = Introspector.getBeanInfo(baseObject.getClass()).getPropertyDescriptors();
                    for (PropertyDescriptor pd: pds) {
                        if (pd.getName().equals(mapKeyOrPropertyName)) {
                            this.propDescriptor = pd;
                            break;
                        }
                    }
                } catch (IntrospectionException e) {
                    throw new IllegalStateException("error introspecting " + baseObject + ": " + e.getLocalizedMessage(), e);
                }
                if (this.propDescriptor == null) {
                    throw new IllegalStateException("property " + mapKeyOrPropertyName + " not found in " + baseObject);
                }
                this.arrayIndex = 0;
                this.mapKey = null;
            }
            this.baseObject = baseObject;
            this.rootClass = null;
            this.pathToMe = new PropertyPath(this, null);
        }

        public Node(Type type, PropertyDescriptor propDescriptor,
                int arrayIndex, String mapKey, Object baseObject) {
            this.type = type;
            this.propDescriptor = propDescriptor;
            this.arrayIndex = arrayIndex;
            this.mapKey = mapKey;
            this.baseObject = baseObject;
            this.rootClass = null;
            this.pathToMe = new PropertyPath(this, null);
        }

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
        public Object getBaseObject() {
            return baseObject;
        }
        
        private void setPathToMe(PropertyPath pathToMe) {
            this.pathToMe = pathToMe;
        }
        
        public PropertyPath getPathToMe() {
            return pathToMe;
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
                if (baseObject.getClass().isArray()) {
                    return baseObject.getClass().getComponentType();
                } else {
                    return Object.class;  //TODO: return the collection's generics component type, if defined
                }
            
            case MAP_VALUE:
                return Object.class; //TODO: return the map's generics value type, if defined
                
            case ROOT:
                return rootClass;
            
            default:
                throw new IllegalStateException("shouldn't happen");
            }
        }
        
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            if (type != Type.PROPERTY) {
                return null;
            }
            Method rm = propDescriptor.getReadMethod();
            if (rm == null) {
                return null;
            }
            try {
                return rm.getAnnotation(annotationClass);
            } catch (Exception e) {
                throw new IllegalStateException("error reading property " + pathToMe + ": " +
                        e.getLocalizedMessage(), e);
            }
        }

        public Annotation[] getAnnotations() {
            if (type != Type.PROPERTY) {
                return new Annotation[0];
            }
            Method rm = propDescriptor.getReadMethod();
            if (rm == null) {
                return new Annotation[0];
            }
            try {
                return rm.getAnnotations();
            } catch (Exception e) {
                throw new IllegalStateException("error reading property " + pathToMe + ": " +
                        e.getLocalizedMessage(), e);
            }
        }

        public Object get() {
            switch (type) {

            case PROPERTY:
                Method rm = propDescriptor.getReadMethod();
                if (rm != null) {
                    try {
                        return rm.invoke(baseObject);
                    } catch (Exception e) {
                        throw new IllegalStateException("error reading property " + pathToMe + ": " +
                                e.getLocalizedMessage(), e);
                    }
                } else {
                    throw new IllegalStateException("property not readable: " + pathToMe);
                }

            case ARRAY_ELEMENT:
                if (baseObject.getClass().isArray()) {
                    return Array.get(baseObject, arrayIndex);
                } else {
                    if (baseObject instanceof List<?>) {
                        return ((List<?>)baseObject).get(arrayIndex);
                    } else {
                        //TODO: this is as inefficient as it gets. Try to optimize
                        //   the common case of consecutive get()s with ascending indexes
                        //   (avoid creating the list, try to cache the iterator over the
                        //   collection)
                        List<?> l = new ArrayList<Object>((Collection<?>) baseObject);
                        return l.get(arrayIndex);
                    }
                }

            case MAP_VALUE:
                return ((Map<?,?>)baseObject).get(mapKey);
            
            case ROOT:
                return rootObject;

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
                        wm.invoke(baseObject, value);
                    } catch (Exception e) {
                        throw new IllegalStateException("error setting property " + pathToMe +
                                " to " + value + ": " + e.getLocalizedMessage(), e);
                    }
                } else {
                    throw new IllegalStateException("property not writable: " + pathToMe);
                }
                break;

            case ARRAY_ELEMENT:
                if (baseObject.getClass().isArray()) {
                    Array.set(baseObject, arrayIndex, value);
                } else {
                    if (baseObject instanceof List<?>) {
                        @SuppressWarnings("unchecked")
                        List<Object> boAsList = (List<Object>) baseObject;
                        while (boAsList.size() <= arrayIndex) {
                            boAsList.add(null);
                        }
                        boAsList.set(arrayIndex, value);
                    } else {
                        //TODO: this is as inefficient as it gets. Try to optimize
                        //   the common case of consecutive set()s with ascending indexes
                        //   (avoid creating the list, add() the new elements directly to
                        //   the collection)
                        List<Object> l = new ArrayList<Object>((Collection<?>) baseObject);
                        l.set(arrayIndex, value);
                    }
                }
                break;

            case MAP_VALUE:
                @SuppressWarnings("unchecked")
                Map<Object,Object> boAsMap = (Map<Object,Object>) baseObject;
                boAsMap.put(mapKey, value);
                break;
            
            case ROOT:
                rootObject = value;
                break;

            default:
                throw new IllegalStateException("shouldn't happen");
            }
        }
        
        @Override
        public String toString() {
            switch (type) {
            case PROPERTY:
                return "." + getPropDescriptor().getName();
            case ARRAY_ELEMENT:
                return "[" + arrayIndex + "]";
            case MAP_VALUE:
                return "[" + mapKey + "]";
            case ROOT:
                return "[" + rootClass.getCanonicalName() + "]";
            default:
                throw new IllegalStateException("shouldn't happen");
            }
        }

    }
    
    
}
