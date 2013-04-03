package de.olafklischat.esmapper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.IdentityHashSet;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.Entity;
import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonMarshaller;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class EntityPersister {
    
    private static final Logger log = Logger.getLogger(EntityPersister.class);

    private Client esClient;
    
    public void setEsClient(Client esClient) {
        this.esClient = esClient;
    }
    
    protected Client getEsClient() {
        if (esClient == null) {
            esClient = createDefaultEsClient();
        }
        return esClient;
    }
    
    protected Client createDefaultEsClient() {
        Node node = NodeBuilder.nodeBuilder().client(true).node();
        return node.client();
    }
    

    /**
     * 
     * @param o
     * @return
     * @throws VersionConflictException if the object was already newer in the database
     */
    public <T extends Entity> T persist(T o) {
        return persist(o, false);
    }

    /**
     * 
     * @param entity
     * @param ignoreVersion if true, persist o even if it wasn't up-to-date
     * @throws VersionConflictException if the object was already newer in the database. Won't happen if ignoreVersion.
     * @return
     */
    public <T extends Entity> T persist(T entity, boolean ignoreVersion) {
        Persister p = new Persister();
        p.setSubObjectsIgnoreVersion(true);  //TODO make configurable
        return p.persist(entity, ignoreVersion);
    }
    
    public <T extends Entity> T findById(String id, Class<T> classOfT) {
        T result;
        try {
            result = classOfT.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("unable to instantiate entity class" + classOfT.getName(), e);
        }
        result.setId(id);
        try {
            load(result);
        } catch (EntityNotFoundException e) {
            return null;
        }
        return result;
    }
    
    public <T extends Entity> void load(T entity) {
        String id = entity.getId();
        if (id == null) {
            throw new IllegalArgumentException("can't load entity with null ID: " + entity);
        }
        GetRequestBuilder grb = getEsClient().prepareGet("esdb", entity.getClass().getSimpleName(), id);
        GetResponse res = grb.execute().actionGet();
        if (!res.exists()) {
            throw new EntityNotFoundException("entity not found: type=" + entity.getClass() + ", id=" + id);
        }
        readJSON(res.getSourceAsString(), entity);
        //entity.setId(res.getId());
        entity.setVersion(res.getVersion());
        entity.setLoaded(true);
    }
    
    @SuppressWarnings("unused")
    private <T extends Entity> T fromJSON(String json, Class<T> classOfT) {
        JsonConverter jsc = new JsonConverter();
        JsonUnmarshallerImpl um = new JsonUnmarshallerImpl();
        jsc.registerUnmarshaller(um);
        return jsc.fromJson(json, classOfT);
    }

    private <T extends Entity> void readJSON(String json, T target) {
        JsonConverter jsc = new JsonConverter();
        JsonUnmarshallerImpl um = new JsonUnmarshallerImpl();
        jsc.registerUnmarshaller(um);
        jsc.readJson(json, target);
    }

    protected class Persister implements JsonMarshaller {
        private boolean subObjectsIgnoreVersion = false;
        private final LinkedList<PropertyPath> entitiesStack = new LinkedList<PropertyPath>();
        private final Set<Entity> seenEntities = new IdentityHashSet<Entity>();

        public boolean isSubObjectsIgnoreVersion() {
            return subObjectsIgnoreVersion;
        }
        
        public void setSubObjectsIgnoreVersion(boolean subObjectsIgnoreVersion) {
            this.subObjectsIgnoreVersion = subObjectsIgnoreVersion;
        }
        
        public <T extends Entity> T persist(T entity, boolean ignoreVersion) {
            String prevId = entity.getId();
            Long prevVersion = entity.getVersion();
            if ((prevId == null) != (prevVersion == null)) {
                throw new IllegalStateException("persisting a non-new object without a version, or a new object with a version, is not supported");
            }
            
            if (log.isDebugEnabled()) {
                log.debug("persisting: " + Joiner.on("=>").join(Lists.reverse(entitiesStack)) + " (" + entity + ")");
            }
            
            JsonConverter jsc = new JsonConverter();
            jsc.registerMarshaller(this);
            String json = jsc.toJson(entity);
            
            IndexRequestBuilder irb = getEsClient().prepareIndex();
            if (prevId != null) {
                irb.setId(prevId);
                if (!ignoreVersion) {
                    irb.setVersion(prevVersion);
                }
                irb.setCreate(false);
            } else {
                irb.setCreate(true);
            }
            irb.setIndex("esdb");
            irb.setType(entity.getClass().getSimpleName());
            irb.setSource(json);
            try {
                IndexResponse res = irb.execute().actionGet();
                entity.setId(res.getId());
                entity.setVersion(res.getVersion());
                entity.setLoaded(true);
            } catch (VersionConflictEngineException esVersionException) {
                throw new VersionConflictException("Version " + entity.getVersion() +
                        " was deprecated (" + esVersionException.getLocalizedMessage() + ")",
                        esVersionException);
            }
            return entity;
        }

        @Override
        public boolean writeJson(PropertyPath sourcePath, JsonWriter out,
                JsonConverter context) throws IOException {
            Object source = sourcePath.get();
            if (!(source instanceof Entity)) {
                //we only handle entities, leave everything else to the default handling
                return false;
            }
            if (sourcePath.getLength() == 1) {
                //we don't handle a root object, even if it is an entity (which it probably is)
                return false;
            }
            Entity e = (Entity) source;
            
            if (! seenEntities.contains(e)) {
                seenEntities.add(e);

                //TODO: recursion may be less robust
                entitiesStack.push(sourcePath);
                try {
                    //TODO: configurability:
                    // - option to only persist non-loaded (new) entities (i.e. no updates)
                    // - ^^ but beware: even non-new entities may reference new ones
                    persist(e, subObjectsIgnoreVersion);
                } finally {
                    entitiesStack.pop();
                }
            }

            out.beginObject();
            out.name("_id");
            out.value(e.getId());
            out.name("_type");
            out.value(e.getClass().getCanonicalName());
            out.endObject();
            return true;
        }
    }
    
    
    protected class JsonUnmarshallerImpl implements JsonUnmarshaller {
        
        @SuppressWarnings("unchecked")
        @Override
        public boolean readJson(JsonElement src, PropertyPath targetPath,
                JsonConverter context) throws IOException {
            Class<?> targetType = targetPath.getNodeClass();
            if (! Entity.class.isAssignableFrom(targetType)) {
                //we only handle entities, leave everything else to the default handling
                return false;
            }
            if (targetPath.getLength() == 1) {
                //we don't handle the root object, even if it is an entity (which it probably is)
                return false;
            }
            if (! src.isJsonObject()) {
                return false;
            }
            JsonObject jso = src.getAsJsonObject();
            if (! jso.has("_type")) {
                //is this an error?
                log.warn("" + targetPath + ": JSON contains an object that's not a reference. Embedded entity?");
                return false;
            }
            String parsedType = jso.get("_type").getAsString();
            Class<? extends Entity> parsedClass;
            try {
                parsedClass = (Class<? extends Entity>) Class.forName(parsedType);
            } catch (Exception e) {
                throw new IllegalStateException("" + targetPath + ": couldn't identify class referenced as _type in JSON (" +
                        parsedType + ")", e);
            }
            if (! targetType.isAssignableFrom(parsedClass)) {
                throw new IllegalStateException("" + targetPath + ": incompatible _type in JSON (" +
                        parsedClass + " <-> " + targetType + ")");
            }
            Entity instance;
            try {
                instance = parsedClass.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("" + targetPath + ": couldn't instatiate class referenced as _type in JSON (" +
                        parsedClass + ")", e);
            }
            String parsedId = jso.get("_id").getAsString();
            instance.setId(parsedId);
            targetPath.set(instance);
            return true;
        }
    }

    
}
