package de.olafklischat.esmapper;

import de.olafklischat.esmapper.annotations.Ignore;

/**
 * Entity type. Instances have their own database identity (as opposed to
 * "embedded" types, which are only stored as part of (embedded in) an entity).
 * 
 * @author olaf
 */
public abstract class Entity {

    /**
     * ID of the entity in the database. null for newly created entities
     * (which don't have a database identity yet).
     * => elasticsearch _id
     */
    private String id;

    /**
     * version of the entity in the database. Starts at one when storing a new
     * entity for the first time, incremented by one on each successive store
     * operation.
     * => elasticsearch _version
     */
    private Long version;
    
    private boolean isLoaded = false;
    
    // TODO _index, _type, _timestamp?

    @Ignore
    public String getId() {
        return id;
    }

    /**
     * Business code shouldn't call this.
     * 
     * @param isLoaded
     */
    public void setId(String id) {
        this.id = id;
    }

    @Ignore
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
    
    @Ignore
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Business code shouldn't call this.
     * 
     * @param isLoaded
     */
    public void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
    }
    
}
