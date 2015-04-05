package de.olafklischat.esmapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonMarshaller;
import de.olafklischat.esmapper.json.JsonMarshallingFilter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class EntityPersister {
    
    private static final Logger log = Logger.getLogger(EntityPersister.class);

    private Client esClient;
    private String indexName;

    public EntityPersister() {
    }

    public EntityPersister(Client esClient, String indexName) {
        this.esClient = esClient;
        this.indexName = indexName;
    }

    public void setEsClient(Client esClient) {
        this.esClient = esClient;
    }
    
    protected Client getEsClient() {
        if (esClient == null) {
            esClient = createDefaultEsClient();
        }
        return esClient;
    }
    
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getIndexName() {
        if (null == indexName) {
            throw new IllegalStateException("EntityPersister: index name not set");
        }
        return indexName;
    }

    protected Client createDefaultEsClient() {
        Node node = NodeBuilder.nodeBuilder().client(true).node();
        return node.client();
    }

    /**
     * Persist an entity, with full cascade, and updating entities even if the
     * in-DB version was newer.
     * 
     * @param entity
     * @return
     */
    public <T> T persist(T entity) {
        return persist(entity, false, CascadeSpec.cascade());
    }

    public <T> T persist(T entity, boolean ignoreVersion) {
        return persist(entity, ignoreVersion, CascadeSpec.cascade());
    }
    
    public <T> T persist(T entity, CascadeSpec ccs) {
        Persister p = new Persister();
        p.setSubObjectsIgnoreVersion(true);  //TODO make configurable
        return p.persist(entity, false, ccs);
    }

    @SuppressWarnings("unchecked")
    public <T> void persist(CascadeSpec ccs, T... entities) {
        Persister p = new Persister();
        p.setSubObjectsIgnoreVersion(true);  //TODO make configurable
        for (T entity : entities) {
            p.persist(entity, false, ccs);
        }
    }

    /**
     * 
     * @param entity
     * @param ignoreVersion if true, persist o even if it wasn't up-to-date
     * @throws VersionConflictException if the object was already newer in the database. Won't happen if ignoreVersion.
     * @return
     */
    public <T> T persist(T entity, boolean ignoreVersion, CascadeSpec ccs) {
        Persister p = new Persister();
        p.setSubObjectsIgnoreVersion(true);  //TODO make configurable
        return p.persist(entity, ignoreVersion, ccs);
    }
    
    public <T> T findById(String id, Class<T> classOfT) {
        return findById(id, classOfT, CascadeSpec.noCascade());
    }
    
    public <T> T findById(String id, Class<T> classOfT, CascadeSpec ccs) {
        return findById(id, classOfT, ccs, new Loader());
    }

    /**
     * 
     * @param <T>
     * @param classOfT
     * @param ccs
     * @param ids
     * @return loaded entities. The map will have a defined order -- that of the ids
     */
    public <T> Map<String, T> findById(Class<T> classOfT, CascadeSpec ccs, String... ids) {
        Loader l = new Loader();
        Map<String, T> result = new LinkedHashMap<String, T>();
        for (String id : ids) {
            result.put(id, findById(id, classOfT, ccs, l));
        }
        return result;
    }

    public Map<String, Object> findById(CascadeSpec ccs, String... ids) {
        Loader l = new Loader();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (String id : ids) {
            result.put(id, findById(id, ccs, l));
        }
        return result;
    }

    protected <T> T findById(String id, Class<T> classOfT, CascadeSpec ccs, Loader l) {
        T result;
        try {
            result = classOfT.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("unable to instantiate entity class " + classOfT.getName(), e);
        }
        EntityIntrospector.setId(result, id);
        try {
            l.load(result, ccs);
        } catch (EntityNotFoundException e) {
            return null;
        }
        return result;
    }
    
    protected Object findById(String id, CascadeSpec ccs, Loader l) {
        Object result;
        GetResponse res = l.readRaw(id, null);
        if (! res.isExists()) {
            return null;
        }
        try {
            // determine class from _class property. disadvantage: requires additional JSON->JsonObject parse step
            // We could determine it from res.getType(), but that would prevent users from storing entities in
            // arbitrary types. We could also use JsonConverter's own polymorphic Object fromJson(String json),
            // which reads the _class internally, but in that case, the root entity would be created by JsonConverter,
            // relatively late in the Json->Object conversion, which would make it a bit awkward to capture the
            // root object and put it into the Loader's seenEntitiesById early enough (it is possible, but the code
            // would be ugly)
            JsonParser p = new JsonParser();
            JsonObject json = p.parse(res.getSourceAsString()).getAsJsonObject();
            result = Class.forName(json.get("_class").getAsString()).newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("unable to instantiate entity class " + res.getType(), e);
        }
        EntityIntrospector.setId(result, id);
        try {
            l.load(result, ccs);
        } catch (EntityNotFoundException e) {
            return null;
        }
        return result;
    }

    public String findRawById(String id, Class<?> classOfT) {
        Loader l = new Loader();
        GetResponse res = l.readRaw(id, classOfT);
        if (!res.isExists()) {
            return null;
        }
        return res.getSourceAsString();
    }

    
    public void load(Object entity) {
        load(entity, CascadeSpec.cascade());
    }
    
    public void load(Object entity, CascadeSpec ccs) {
        Loader l = new Loader();
        l.load(entity, ccs);
    }
    
    public void load(CascadeSpec ccs, Object... entities) {
        Loader l = new Loader();
        for (Object entity : entities) {
            l.load(entity, ccs);
        }
    }

    protected class Persister implements JsonMarshaller, JsonMarshallingFilter {
        private boolean subObjectsIgnoreVersion = false;
        private final LinkedList<PropertyPath> entitiesStack = new LinkedList<PropertyPath>();
        private final Set<Object> seenEntities = new IdentityHashSet<Object>();  //TODO: hash by ID rather than identity?

        public boolean isSubObjectsIgnoreVersion() {
            return subObjectsIgnoreVersion;
        }
        
        public void setSubObjectsIgnoreVersion(boolean subObjectsIgnoreVersion) {
            this.subObjectsIgnoreVersion = subObjectsIgnoreVersion;
        }
        
        public <T> T persist(T entity, boolean ignoreVersion, CascadeSpec cascadeSpec) {
            String prevId = EntityIntrospector.getId(entity);
            boolean supportsVersion = EntityIntrospector.supportsVersion(entity);
            Long prevVersion = supportsVersion ? EntityIntrospector.getVersion(entity) : null;
            if (supportsVersion && ((prevId == null) != (prevVersion == null))) {
                throw new IllegalStateException("persisting a non-new object without a version, or a new object with a version, is not supported");
            }
            
            if (log.isDebugEnabled()) {
                log.debug("persisting: " + Joiner.on("=>").join(Lists.reverse(entitiesStack)) + " (" + entity + ")");
            }
            
            seenEntities.add(entity);
            
            IndexRequestBuilder irb = getEsClient().prepareIndex();
            if (prevId != null) {
                irb.setId(prevId);
                if (!ignoreVersion) {
                    irb.setVersion(prevVersion);
                }
                irb.setCreate(false);
            } else {
                String id = UUID.randomUUID().toString();
                irb.setId(id);
                EntityIntrospector.setId(entity, id);
                irb.setCreate(true);
            }
            irb.setIndex(getIndexName());
            irb.setType(entity.getClass().getSimpleName());
            JsonConverter jsc = new JsonConverter();
            jsc.registerMarshaller(this);
            jsc.registerMarshallingFilter(this);
            jsc.setAttribute("cascadeSpec", cascadeSpec);
            String json = jsc.toJson(entity);
            irb.setSource(json);
            try {
                IndexResponse res = irb.execute().actionGet();
                EntityIntrospector.setId(entity, res.getId());
                if (supportsVersion) {
                    EntityIntrospector.setVersion(entity, res.getVersion());
                }
                if (EntityIntrospector.supportsLoadedFlag(entity)) {
                    EntityIntrospector.setLoaded(entity, true);
                }
            } catch (VersionConflictEngineException esVersionException) {
                throw new VersionConflictException("Version " + EntityIntrospector.getVersion(entity) +
                        " was deprecated (" + esVersionException.getLocalizedMessage() + ")",
                        esVersionException);
            }
            return entity;
        }

        @Override
        public boolean writeJson(PropertyPath sourcePath, JsonWriter out,
                JsonConverter context) throws IOException {
            Object e = sourcePath.get();
            if (e == null || !(EntityIntrospector.isEntity(e))) {
                //we only handle entities, leave everything else to the default handling
                return false;
            }
            if (sourcePath.getLength() == 1) {
                //we don't handle a root object, even if it is an entity (which it probably is)
                return false;
            }
            
            if (seenEntities.contains(e)) {
                writeReference(out, e);
                return true;
            }
            
            boolean ePersisted = false;
            CascadeSpec currSpec = (CascadeSpec) context.getAttribute("cascadeSpec");
            CascadeSpec subSpec = currSpec.getEffectiveSubSpecFor(sourcePath.getPathNotation());
            if (subSpec.isDefaultCascade()) {
                //TODO: recursion may be less robust
                entitiesStack.push(sourcePath);
                try {
                    //TODO: configurability:
                    // - option to only persist non-loaded (new) entities (i.e. no updates)
                    // - ^^ but beware: even non-new entities may reference new ones
                    persist(e, subObjectsIgnoreVersion, subSpec);
                    writeReference(out, e);
                    ePersisted = true;
                } finally {
                    entitiesStack.pop();
                    if (!ePersisted) {
                        //exception was thrown
                        if (EntityIntrospector.getId(e) != null) {
                            writeReference(out, e);
                        } else {
                            out.nullValue();
                        }
                    }
                }
            }

            if (!ePersisted) {
                if (EntityIntrospector.getId(e) != null) {
                    writeReference(out, e);
                } else {
                    out.nullValue();
                }
            }

            return true;
        }
        
        @Override
        public boolean shouldMarshal(PropertyPath sourcePath,
                JsonConverter context) {
            // avoid marshalling entity metadata properties (id, version, loaded)
            Object bo = sourcePath.getBaseObject();
            return !(EntityIntrospector.isEntity(bo) &&
                     sourcePath.getHead().isPropertyAccess() &&
                     EntityIntrospector.isMetadataProperty(bo, sourcePath.getHead().getPropDescriptor().getName()));
        }
        
        private void writeReference(JsonWriter out, Object e) throws IOException {
            out.beginObject();
            out.name("_ref_id");
            out.value(EntityIntrospector.getId(e));
            out.name("_ref_class");
            out.value(e.getClass().getCanonicalName());
            out.endObject();
        }
    }
    
    
    protected class Loader implements JsonUnmarshaller {

        private final LinkedList<PropertyPath> entitiesStack = new LinkedList<PropertyPath>();
        //we assume the IDs are unique globally, not just per-type
        private final Map<String, Object> seenEntitiesById = new HashMap<String, Object>();

        public void load(Object entity, CascadeSpec cascadeSpec) {
            String id = EntityIntrospector.getId(entity);
            if (id == null) {
                throw new IllegalArgumentException("can't load entity with null ID: " + entity);
            }
            GetResponse res = readRaw(id, entity.getClass());
            load(res, entity, cascadeSpec);
        }

        public <T> void load(GetResponse res, T entity, CascadeSpec cascadeSpec) {
            String id = EntityIntrospector.getId(entity);
            if (id == null) {
                throw new IllegalArgumentException("can't load entity with null ID: " + entity);
            }
            if (log.isDebugEnabled()) {
                log.debug("loading: " + Joiner.on("=>").join(Lists.reverse(entitiesStack)) + " (" + entity + ", id=" + id + ")");
            }
            if (seenEntitiesById.containsKey(id)) {
                return;
            }
            seenEntitiesById.put(id, entity);
            if (!res.isExists()) {
                throw new EntityNotFoundException("entity not found: type=" + entity.getClass() + ", id=" + id);
            }
            JsonConverter jsc = new JsonConverter();
            jsc.registerUnmarshaller(this);
            jsc.setAttribute("cascadeSpec", cascadeSpec);
            jsc.readJson(res.getSourceAsString(), entity);
            //EntityIntrospector.setId(entity, res.getId());
            if (EntityIntrospector.supportsVersion(entity)) {
                EntityIntrospector.setVersion(entity, res.getVersion());
            }
            if (EntityIntrospector.supportsLoadedFlag(entity)) {
                EntityIntrospector.setLoaded(entity, true);
            }
        }
        
        public GetResponse readRaw(String id, Class<?> classOfT) {
            GetRequestBuilder grb = getEsClient().prepareGet(getIndexName(), classOfT == null ? null : classOfT.getSimpleName(), id);
            return grb.execute().actionGet();
        }
        
        @Override
        public boolean readJson(JsonElement src, PropertyPath targetPath,
                JsonConverter context) throws IOException {
            if (targetPath.getLength() == 1) {
                //we don't handle the root object, even if it is an entity (which it probably is)
                return false;
            }
            if (! src.isJsonObject()) {
                return false;
            }
            JsonObject jso = src.getAsJsonObject();
            if (! (jso.has("_ref_class") && jso.has("_ref_id"))) {
                return false;
            }
            Class<?> targetClass = targetPath.getNodeClass();
            if (! EntityIntrospector.isEntity(targetClass)) {
                //we only handle entities, leave everything else to the default handling
                //can't do this test atm. because getNodeClass() may return Object.class for Collection generics component types
                //return false;
            }
            String parsedClassName = jso.get("_ref_class").getAsString();
            String parsedId = jso.get("_ref_id").getAsString();
            Object instance = seenEntitiesById.get(parsedId);
            if (instance != null) {
                targetPath.set(instance);
                return true;
            }
            Class<?> parsedClass;
            try {
                parsedClass = Class.forName(parsedClassName);
            } catch (Exception e) {
                throw new IllegalStateException("" + targetPath + ": couldn't identify class referenced as _ref_class in JSON (" +
                        parsedClassName + ")", e);
            }
            if (! targetClass.isAssignableFrom(parsedClass)) {
                throw new IllegalStateException("" + targetPath + ": incompatible _ref_class in JSON (" +
                        parsedClass + " <-> " + targetClass + ")");
            }
            try {
                instance = parsedClass.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("" + targetPath + ": couldn't instantiate class referenced as _ref_class in JSON (" +
                        parsedClass + ")", e);
            }
            EntityIntrospector.setId(instance, parsedId);
            targetPath.set(instance);
            CascadeSpec currSpec = (CascadeSpec) context.getAttribute("cascadeSpec");
            CascadeSpec subSpec = currSpec.getEffectiveSubSpecFor(targetPath.getPathNotation());
            if (subSpec.isDefaultCascade()) {
                entitiesStack.push(targetPath);
                try {
                    load(instance, subSpec);
                } catch (EntityNotFoundException e) {
                    //TODO: make this configurable via the CascadeSpec
                    //targetPath.set(null); //(alternative) set to null rather than a stub
                    log.warn("referenced entity not found: " + targetPath + " (id=" + parsedId + "). Reference set to stub (non-loaded) entity.", e);
                } finally {
                    entitiesStack.pop();
                }
            }
            return true;
        }
    }

    
}
