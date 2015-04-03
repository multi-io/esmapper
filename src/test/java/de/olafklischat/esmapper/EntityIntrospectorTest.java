package de.olafklischat.esmapper;

import static org.junit.Assert.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import org.junit.Test;

import de.olafklischat.esmapper.annotations.LoadedFlag;

@Entity
class BeanAllPropsNoAnns {
    private String id;
    private Long version;
    private boolean isLoaded = false;
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Long getVersion() {
        return version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }
    public boolean isLoaded() {
        return isLoaded;
    }
    public void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
    }
}

@Entity
class BeanAllPropsAnns {
    private String id;
    private Long version;
    private boolean isLoaded = false;

    private String realId;
    private Long realVersion;
    private boolean realLoaded = false;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Long getVersion() {
        return version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }
    public boolean isLoaded() {
        return isLoaded;
    }
    public void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
    }
    @Id
    public String getRealId() {
        return realId;
    }
    public void setRealId(String realId) {
        this.realId = realId;
    }
    @Version
    public Long getRealVersion() {
        return realVersion;
    }
    public void setRealVersion(Long realVersion) {
        this.realVersion = realVersion;
    }
    @LoadedFlag
    public boolean isRealLoaded() {
        return realLoaded;
    }
    public void setRealLoaded(boolean realLoaded) {
        this.realLoaded = realLoaded;
    }
}

@Entity
class BeanNoIdNoAnns {
    private Long version;
    private boolean isLoaded = false;
    
    public Long getVersion() {
        return version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }
    public boolean isLoaded() {
        return isLoaded;
    }
    public void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
    }
}

@Entity
class BeanOnlyId {
    private String id;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}

public class EntityIntrospectorTest {

    @Test
    public void testDefaultId() {
        BeanAllPropsNoAnns ent = new BeanAllPropsNoAnns();
        assertTrue(EntityIntrospector.isEntity(ent));
        assertNull(ent.getId());
        assertNull(EntityIntrospector.getId(ent));
        ent.setId("123");
        assertEquals("123", EntityIntrospector.getId(ent));
        EntityIntrospector.setId(ent, "456");
        assertEquals("456", ent.getId());
        assertEquals("456", EntityIntrospector.getId(ent));
        assertTrue(EntityIntrospector.supportsVersion(ent));
        assertTrue(EntityIntrospector.supportsLoadedFlag(ent));
    }

    @Test
    public void testDefaultVersion() {
        BeanAllPropsNoAnns ent = new BeanAllPropsNoAnns();
        assertTrue(EntityIntrospector.isEntity(ent));
        assertNull(ent.getVersion());
        assertNull(EntityIntrospector.getVersion(ent));
        ent.setVersion(23L);
        assertEquals((Long)23L, EntityIntrospector.getVersion(ent));
        EntityIntrospector.setVersion(ent, 42L);
        assertEquals((Long)42L, ent.getVersion());
        assertEquals((Long)42L, EntityIntrospector.getVersion(ent));
    }

    @Test
    public void testDefaultLoaded() {
        BeanAllPropsNoAnns ent = new BeanAllPropsNoAnns();
        assertTrue(EntityIntrospector.isEntity(ent));
        assertFalse(ent.isLoaded());
        assertFalse(EntityIntrospector.isLoaded(ent));
        ent.setLoaded(true);
        assertTrue(EntityIntrospector.isLoaded(ent));
        EntityIntrospector.setLoaded(ent, false);
        assertFalse(ent.isLoaded());
        assertFalse(EntityIntrospector.isLoaded(ent));
    }

    @Test
    public void testAnnotatedId() {
        BeanAllPropsAnns ent = new BeanAllPropsAnns();
        assertTrue(EntityIntrospector.isEntity(ent));
        assertNull(ent.getId());
        assertNull(ent.getRealId());
        assertNull(EntityIntrospector.getId(ent));
        ent.setRealId("123");
        assertNull(ent.getId());
        assertEquals("123", EntityIntrospector.getId(ent));
        assertEquals("123", ent.getRealId());
        EntityIntrospector.setId(ent, "456");
        assertEquals("456", ent.getRealId());
        assertEquals("456", EntityIntrospector.getId(ent));
        assertNull(ent.getId());
        ent.setId("111");
        EntityIntrospector.setId(ent, "789");
        assertEquals("789", ent.getRealId());
        assertEquals("789", EntityIntrospector.getId(ent));
        assertEquals("111", ent.getId());
        assertTrue(EntityIntrospector.supportsVersion(ent));
        assertTrue(EntityIntrospector.supportsLoadedFlag(ent));
    }

    @Test
    public void testAnnotatedVersion() {
        BeanAllPropsAnns ent = new BeanAllPropsAnns();
        assertTrue(EntityIntrospector.isEntity(ent));
        assertNull(ent.getVersion());
        assertNull(ent.getRealVersion());
        assertNull(EntityIntrospector.getVersion(ent));
        ent.setRealVersion(123L);
        assertNull(ent.getVersion());
        assertEquals((Long)123L, EntityIntrospector.getVersion(ent));
        assertEquals((Long)123L, ent.getRealVersion());
        EntityIntrospector.setVersion(ent, 456L);
        assertEquals((Long)456L, ent.getRealVersion());
        assertEquals((Long)456L, EntityIntrospector.getVersion(ent));
        assertNull(ent.getVersion());
        ent.setVersion(111L);
        EntityIntrospector.setVersion(ent, 789L);
        assertEquals((Long)789L, ent.getRealVersion());
        assertEquals((Long)789L, EntityIntrospector.getVersion(ent));
        assertEquals((Long)111L, ent.getVersion());
        assertTrue(EntityIntrospector.supportsVersion(ent));
        assertTrue(EntityIntrospector.supportsLoadedFlag(ent));
    }

    @Test
    public void testAnnotatedLoaded() {
        BeanAllPropsAnns ent = new BeanAllPropsAnns();
        assertTrue(EntityIntrospector.isEntity(ent));
        assertFalse(ent.isLoaded());
        assertFalse(ent.isRealLoaded());
        assertFalse(EntityIntrospector.isLoaded(ent));
        ent.setRealLoaded(true);
        assertFalse(ent.isLoaded());
        assertTrue(EntityIntrospector.isLoaded(ent));
        assertTrue(ent.isRealLoaded());
        EntityIntrospector.setLoaded(ent, false);
        ent.setLoaded(true);
        assertFalse(ent.isRealLoaded());
        assertFalse(EntityIntrospector.isLoaded(ent));
        assertTrue(ent.isLoaded());
    }

    @Test
    public void testIdRequired() {
        BeanNoIdNoAnns ent = new BeanNoIdNoAnns();
        assertTrue(EntityIntrospector.isEntity(ent));
        try {
            EntityIntrospector.getVersion(ent);
            fail("exception (id property missing) expected");
        } catch (IllegalStateException e) {
            
        }
    }

    @Test
    public void testPropAbsence() {
        BeanOnlyId ent = new BeanOnlyId();
        assertTrue(EntityIntrospector.isEntity(ent));
        EntityIntrospector.setId(ent, "123");
        assertEquals("123", ent.getId());
        assertFalse(EntityIntrospector.supportsVersion(ent));
        assertFalse(EntityIntrospector.supportsLoadedFlag(ent));
    }
}
