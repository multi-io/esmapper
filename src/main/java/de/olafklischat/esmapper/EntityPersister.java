package de.olafklischat.esmapper;

import java.lang.reflect.ParameterizedType;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.gson.Gson;

import de.olafklischat.esmapper.Entity;

public class EntityPersister<EntityType extends Entity> {

    /**
     * EntityType (generics parameter), but available at runtime
     */
    private final Class<EntityType> entityType;
    
    private Client esClient;

    /**
     * When instantiating this class directly, you have to pass the entity type (the generic parameter)
     * to the constructor because otherwise it wouldn't be available at runtime due to limitations to
     * Java's generics implementation (type erasure).
     * 
     * @param entityType
     */
    public EntityPersister(Class<EntityType> entityType) {
        this.entityType = entityType;
    }
    
    /**
     * C'tor to be used by subclasses. Determines the entity type internally from generics information
     * written into the subclass's .class file by the compiler.
     */
    @SuppressWarnings("unchecked")
    protected EntityPersister() {
        ParameterizedType superclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        if (superclass == null || ! superclass.getRawType().equals(EntityPersister.class) || superclass.getActualTypeArguments().length != 1) {
            throw new IllegalArgumentException("Can't determine entity type at runtime when" +
                    " NormizeEntityPersister isn't used as a base class. Pass the entity type to the constructor explicitly");
        }
        this.entityType = (Class<EntityType>) superclass.getActualTypeArguments()[0];
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
    public EntityType persist(EntityType o) {
        return persist(o, false);
    }

    /**
     * 
     * @param entity
     * @param ignoreVersion if true, persist o even if it wasn't up-to-date
     * @throws VersionConflictException if the object was already newer in the database. Won't happen if ignoreVersion.
     * @return
     */
    public EntityType persist(EntityType entity, boolean ignoreVersion) {
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
        irb.setType(entityType.getSimpleName());
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

    public EntityType findById(String id) {
        GetRequestBuilder grb = getEsClient().prepareGet("esdb", entityType.getSimpleName(), id);
        GetResponse res = grb.execute().actionGet();
        if (!res.exists()) {
            return null;
        }
        EntityType result = fromJSON(res.getSourceAsString());
        result.setId(res.getId());
        result.setVersion(res.getVersion());
        return result;
    }
    
    private String toJSON(EntityType entity) {
        Gson gson = new Gson();
        return gson.toJson(entity);  //TODO: ignore id, version, handle associations...
    }
    
    private EntityType fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, entityType);  //TODO: handle associations...
    }

}
