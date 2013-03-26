package de.olafklischat.esmapper;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import de.olafklischat.esmapper.Entity;
import de.olafklischat.esmapper.json.JsonConverter;

public class EntityPersister {

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
        String prevId = entity.getId();
        Long prevVersion = entity.getVersion();
        if ((prevId == null) != (prevVersion == null)) {
            throw new IllegalStateException("persisting a non-new object without a version, or a new object with a version, is not supported");
        }
        IndexRequestBuilder irb = getEsClient().prepareIndex();
        if (prevId != null) {
            irb.setId(prevId);
            if (!ignoreVersion) {
                irb.setVersion(prevVersion);
            }
            irb.setCreate(false);
        } else {
            //TODO: irb.setId(generateUniqueId()); to avoid ID autogeneration by ES
            irb.setCreate(true);
        }
        irb.setIndex("esdb");
        irb.setType(entity.getClass().getSimpleName());
        irb.setSource(toJSON(entity));
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
    
    private <T extends Entity> String toJSON(T entity) {
        JsonConverter jsc = new JsonConverter();
        return jsc.toJson(entity);
    }
    
    private <T extends Entity> T fromJSON(String json, Class<T> classOfT) {
        JsonConverter jsc = new JsonConverter();
        return jsc.fromJson(json, classOfT);
    }

    private <T extends Entity> void readJSON(String json, T target) {
        JsonConverter jsc = new JsonConverter();
        jsc.readJson(json, target);
    }

}
