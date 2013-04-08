package de.olafklischat.esmapper;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.*;

public class EntityPersisterTest {

    @SuppressWarnings("unused")
    private Node localDB;
    private EntityPersister ep;
    
    @Before
    public void setUp() throws Exception {
        NodeBuilder localDBBuilder = NodeBuilder.nodeBuilder().local(true).client(false).data(true);
        localDBBuilder.settings().put("index.store.type", "memory"); //TODO doesn't work -- it still creates the index in the FS
        assertEquals("memory", localDBBuilder.settings().get("index.store.type"));
        
        localDB = localDBBuilder.node();

        Node localClient = NodeBuilder.nodeBuilder().local(true).client(true).node();
        ep = new EntityPersister();
        ep.setEsClient(localClient.client());
    }
    
    @Test
    public void testSimpleStoreLoad() {
        TestPerson e = new TestPerson();
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
        
        TestPerson e2 = ep.findById(e.getId(), TestPerson.class);
        assertEquals(42, e2.getAge());
        assertEquals(e, e2);
        assertTrue(e2.isLoaded());
        
        e.setAge(44);
        ep.persist(e);
        assertEquals(e2.getId(), e.getId()); //ID hasn't changed
        assertEquals(new Long(2), e.getVersion()); //version has incremented
        assertTrue(e.isLoaded());

        TestPerson e3 = ep.findById(e.getId(), TestPerson.class);
        assertEquals(44, e3.getAge());
        assertEquals(new Long(2), e3.getVersion());
        assertEquals(e, e3);
        
        TestPerson e4 = new TestPerson();
        e4.setId(e.getId());
        ep.load(e4);
        assertEquals(e, e4);
        assertTrue(e4.isLoaded());
    }
    
    @Test
    public void testNotFound() {
        ep.persist(new TestPerson("hans", 21, "foo bar baz"));  //make sure the index exists...

        TestPerson e2 = ep.findById("xxx-doesnt-exist-xxx", TestPerson.class);
        assertNull(e2);
    }
    
    @Test
    public void testVersionConflict() {
        TestPerson e = new TestPerson();
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
        
        TestPerson e2 = ep.findById(e.getId(), TestPerson.class);
        assertEquals(e, e2);
        assertTrue(e2.isLoaded());
        
        e.setName("hugo");
        e.setAge(44);
        ep.persist(e);
        assertEquals(e2.getId(), e.getId()); //ID hasn't changed
        assertEquals(new Long(2), e.getVersion()); //version has incremented
        assertTrue(e.isLoaded());

        assertEquals(ep.findById(e.getId(), TestPerson.class), e);
        
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

        TestPerson e3 = ep.findById(e.getId(), TestPerson.class);
        assertEquals(46, e3.getAge());
        assertEquals(new Long(3), e3.getVersion());
        assertEquals(e, e3);
        assertTrue(e3.isLoaded());
    }
    
    @Test
    public void testStoreLoadRelationNoCascade() {
        TestCity c = new TestCity("Berlin", 3500000);
        ep.persist(c);
        assertNotNull(c.getId());
        assertTrue(c.isLoaded());
        TestPerson p = new TestPerson("paul", 42, "nice guy");
        p.setHomeTown(c);
        ep.persist(p);
        
        TestPerson p2 = ep.findById(p.getId(), TestPerson.class);
        assertEquals(p, p2);
        TestCity c2 = p2.getHomeTown();
        assertFalse(c2.isLoaded());
        assertNull(c2.getName());
        ep.load(c2);
        assertTrue(c2.isLoaded());
        assertEquals("Berlin", c2.getName());
        assertEquals(c, c2);
    }
    
    @Test
    public void testStoreLoadRelationFullCascade() {
        TestCity c = new TestCity("Berlin", 3500000);
        ep.persist(c);
        assertNotNull(c.getId());
        assertTrue(c.isLoaded());
        TestPerson p = new TestPerson("paul", 42, "nice guy");
        p.setHomeTown(c);
        ep.persist(p);
        
        TestPerson p2 = ep.findById(p.getId(), TestPerson.class, CascadeSpec.FULL_CASCADE);
        assertEquals(p, p2);
        TestCity c2 = p2.getHomeTown();
        assertTrue(c2.isLoaded());
        assertEquals("Berlin", c2.getName());
        assertEquals(c, c2);
    }

    @Test
    public void testStoreLoadNestedRelationWithCyclesFullCascade() {
        TestCity liv = new TestCity("Liverpool", 12345);
        TestCity ldn = new TestCity("London", 678);
        TestCity mch = new TestCity("Manchester", 9012);
        liv.setSisterCities(Lists.newArrayList(ldn, mch));
        mch.setSisterCities(Lists.newArrayList(liv));
        TestPerson paul = new TestPerson("paul", 65, "nice guy");
        TestPerson john = new TestPerson("john", 67, "dead guy");
        //TestPerson george = new TestPerson("george", 69, "one hit wonder");
        paul.setHomeTown(liv);
        liv.setMayor(john);
        //george.setHomeTown(mch);
        
        assertFalse(paul.isLoaded());
        assertNull(paul.getId());
        assertFalse(john.isLoaded());
        assertNull(john.getId());
        assertFalse(liv.isLoaded());
        assertFalse(ldn.isLoaded());
        assertNull(ldn.getId());
        
        ep.persist(paul);
        
        assertTrue(paul.isLoaded());
        assertNotNull(paul.getId());
        assertTrue(john.isLoaded());
        assertNotNull(john.getId());
        assertTrue(liv.isLoaded());
        assertTrue(ldn.isLoaded());
        assertNotNull(ldn.getId());
        assertNotNull(mch.getId());
        
        //white-box test to inspect the circular back reference from mch.ss[0] to liv
        JsonParser p = new JsonParser();
        JsonObject mchJson = p.parse(ep.findRawById(mch.getId(), TestCity.class)).getAsJsonObject();
        JsonObject mchSSref = mchJson.get("sisterCities").getAsJsonArray().get(0).getAsJsonObject();
        assertEquals(liv.getClass().getCanonicalName(), mchSSref.get("_type").getAsString());
        assertEquals(liv.getId(), mchSSref.get("_id").getAsString());
        
        TestCity mch2 = ep.findById(mch.getId(), TestCity.class, CascadeSpec.NO_CASCADE);
        TestCity mch2SS = mch2.getSisterCities().toArray(new TestCity[2])[0];
        assertFalse(mch2SS.isLoaded());
        assertEquals(liv.getId(), mch2SS.getId());
        
        TestPerson paul2 = ep.findById(paul.getId(), TestPerson.class, CascadeSpec.FULL_CASCADE);
        assertEquals(paul, paul2);
        assertEquals(liv, paul2.getHomeTown());
        assertEquals(john, paul2.getHomeTown().getMayor());
        TestCity[] paul2htSS = paul2.getHomeTown().getSisterCities().toArray(new TestCity[2]);
        assertEquals(ldn, paul2htSS[0]);
        assertEquals(mch, paul2htSS[1]);
        assertEquals(liv, paul2htSS[1].getSisterCities().toArray()[0]);
    }

    @After
    public void tearDown() throws Exception {
    }

}
