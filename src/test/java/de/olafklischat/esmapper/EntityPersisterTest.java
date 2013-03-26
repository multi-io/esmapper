package de.olafklischat.esmapper;


import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntityPersisterTest {

    @SuppressWarnings("unused")
    private Node localDB;
    private EntityPersister ep;
    
    @Before
    public void setUp() throws Exception {
        NodeBuilder localDBBuilder = NodeBuilder.nodeBuilder().local(true).client(false).data(true);
        localDBBuilder.settings().put("index.store.type", "memory"); //TODO doesn't work -- in still creates the index in the FS
        assertEquals("memory", localDBBuilder.settings().get("index.store.type"));
        
        localDB = localDBBuilder.node();

        Node localClient = NodeBuilder.nodeBuilder().local(true).client(true).node();
        ep = new EntityPersister();
        ep.setEsClient(localClient.client());
    }
    
    @Test
    public void testSimpleStoreLoad() {
        TestEntity e = new TestEntity();
        e.setName("hans");
        assertEquals("hans", e.getName());
        e.setAge(42);
        e.setComment("foo bar");
        assertNull(e.getId());
        assertNull(e.getVersion());
        assertFalse(e.isLoaded());

        ep.persist(e);
        
        assertNotNull(e.getId());
        assertEquals(new Long(1), e.getVersion());
        assertTrue(e.isLoaded());
        
        TestEntity e2 = ep.findById(e.getId(), TestEntity.class);
        assertEquals(42, e2.getAge());
        assertEquals(e, e2);
        assertTrue(e2.isLoaded());
        
        e.setAge(44);
        ep.persist(e);
        assertEquals(e2.getId(), e.getId()); //ID hasn't changed
        assertEquals(new Long(2), e.getVersion()); //version has incremented
        assertTrue(e.isLoaded());

        TestEntity e3 = ep.findById(e.getId(), TestEntity.class);
        assertEquals(44, e3.getAge());
        assertEquals(new Long(2), e3.getVersion());
        assertEquals(e, e3);
    }
    
    @Test
    public void testNotFound() {
        ep.persist(new TestEntity("hans", 21, "foo bar baz"));  //make sure the index exists...

        TestEntity e2 = ep.findById("xxx-doesnt-exist-xxx", TestEntity.class);
        assertNull(e2);
    }
    
    @Test
    public void testVersionConflict() {
        TestEntity e = new TestEntity();
        e.setName("hans");
        assertEquals("hans", e.getName());
        e.setAge(42);
        e.setComment("foo bar");
        assertNull(e.getId());
        assertNull(e.getVersion());
        assertFalse(e.isLoaded());

        ep.persist(e);
        
        assertNotNull(e.getId());
        assertEquals(new Long(1), e.getVersion());
        assertTrue(e.isLoaded());
        
        TestEntity e2 = ep.findById(e.getId(), TestEntity.class);
        assertEquals(e, e2);
        assertTrue(e2.isLoaded());
        
        e.setName("hugo");
        e.setAge(44);
        ep.persist(e);
        assertEquals(e2.getId(), e.getId()); //ID hasn't changed
        assertEquals(new Long(2), e.getVersion()); //version has incremented
        assertTrue(e.isLoaded());

        assertEquals(ep.findById(e.getId(), TestEntity.class), e);
        
        e.setAge(46);
        e.setVersion(1L); //try to store against deprecated version...
        try {
            ep.persist(e); //...should fail...
            fail("VersionConflictException expected.");
        } catch (VersionConflictException ex) {
        }
        
        ep.persist(e, true); //...unless we ignore version conflicts.
        assertEquals(e2.getId(), e.getId());
        assertEquals(new Long(3), e.getVersion());
        assertTrue(e2.isLoaded());

        TestEntity e3 = ep.findById(e.getId(), TestEntity.class);
        assertEquals(46, e3.getAge());
        assertEquals(new Long(3), e3.getVersion());
        assertEquals(e, e3);
        assertTrue(e3.isLoaded());
    }
    
    @After
    public void tearDown() throws Exception {
    }

}
