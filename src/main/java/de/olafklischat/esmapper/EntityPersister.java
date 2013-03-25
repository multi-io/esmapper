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
        } catch (VersionConflictEngineException esVersionException) {
            throw new VersionConflictException("Version " + entity.getVersion() +
                    " was deprecated (" + esVersionException.getLocalizedMessage() + ")",
                    esVersionException);
        }
        return entity;
    }

    public <T extends Entity> T findById(String id, Class<T> classOfT) {
        GetRequestBuilder grb = getEsClient().prepareGet("esdb", classOfT.getSimpleName(), id);
        GetResponse res = grb.execute().actionGet();
        if (!res.exists()) {
            return null;
        }
        T result = fromJSON(res.getSourceAsString(), classOfT);
        result.setId(res.getId());
        result.setVersion(res.getVersion());
        return result;
    }
    
    private <T extends Entity> String toJSON(T entity) {
        JsonConverter jsc = new JsonConverter();
        return jsc.toJson(entity);
    }
    
    private <T extends Entity> T fromJSON(String json, Class<T> classOfT) {
        JsonConverter jsc = new JsonConverter();
        return jsc.fromJson(json, classOfT);
    }

}
