package de.olafklischat.esmapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.olafklischat.esmapper.Entity;

public class EntityPersisterTest {

    private static ESRunner esRunner;
    private static Node esClient;

    private EntityPersister ep;
    
    @BeforeClass
    public static void classSetUp() throws Exception {
        esRunner = new ESRunner(6200);
        esRunner.setClusterName("esmapper_testcluster");
        if (! esRunner.isRunning()) {
            esRunner.startLocally();
        }
        esClient = esRunner.createClient();
    }
    
    @Before
    public void setUp() throws Exception {
        ep = new EntityPersister();
        ep.setEsClient(esClient.client());
    }
    
    public static void assertLoaded(Entity e) {
        assertTrue(e.isLoaded());
        assertNotNull(e.getId());
    }

    public static void assertNotLoaded(Entity e) {
        assertFalse(e.isLoaded());
        assertNull(e.getId());
    }

    public static void assertIsStub(Entity e) {
        assertFalse(e.isLoaded());
        assertNotNull(e.getId());
    }

    public static void assertEqualsIncludingId(Entity e1, Entity e2) {
        assertEquals(e1, e2);
        assertEquals(e1.getId(), e2.getId());
    }

    @Test
    public void testSimpleStoreLoad() {
        TestPerson e = new TestPerson();
        e.setName("hans");
        assertEquals("hans", e.getName());
        e.setAge(42);
        e.setComment("foo bar");
        assertNotLoaded(e);
        assertNull(e.getVersion());

        ep.persist(e);
        
        assertLoaded(e);
        assertEquals(new Long(1), e.getVersion());
        
        TestPerson e2 = ep.findById(e.getId(), TestPerson.class);
        assertEquals(42, e2.getAge());
        assertEquals(e, e2);
        assertLoaded(e2);
        
        e.setAge(44);
        ep.persist(e);
        assertEquals(e2.getId(), e.getId()); //ID hasn't changed
        assertEquals(new Long(2), e.getVersion()); //version has incremented
        assertLoaded(e);

        TestPerson e3 = ep.findById(e.getId(), TestPerson.class);
        assertEquals(44, e3.getAge());
        assertEquals(new Long(2), e3.getVersion());
        assertEquals(e, e3);
        
        TestPerson e4 = new TestPerson();
        e4.setId(e.getId());
        ep.load(e4);
        assertEquals(e, e4);
        assertLoaded(e4);
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
        assertNotLoaded(e);
        assertNull(e.getVersion());

        ep.persist(e);
        
        assertLoaded(e);
        assertEquals(new Long(1), e.getVersion());
        
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
        assertLoaded(c);
        TestPerson p = new TestPerson("paul", 42, "nice guy");
        p.setHomeTown(c);
        ep.persist(p);
        
        TestPerson p2 = ep.findById(p.getId(), TestPerson.class);
        assertEquals(p, p2);
        TestCity c2 = p2.getHomeTown();
        assertIsStub(c2);
        assertNull(c2.getName());
        ep.load(c2);
        assertLoaded(c2);
        assertEquals("Berlin", c2.getName());
        assertEquals(c, c2);
    }
    
    @Test
    public void testStoreLoadRelationFullCascade() {
        TestCity c = new TestCity("Berlin", 3500000);
        ep.persist(c);
        assertLoaded(c);
        TestPerson p = new TestPerson("paul", 42, "nice guy");
        p.setHomeTown(c);
        ep.persist(p);
        
        TestPerson p2 = ep.findById(p.getId(), TestPerson.class, CascadeSpec.cascade());
        assertEquals(p, p2);
        TestCity c2 = p2.getHomeTown();
        assertLoaded(c2);
        assertEquals("Berlin", c2.getName());
        assertEquals(c, c2);
    }


    private static class TestObjectGraph {
        TestPerson john, paul;
        TestCity liv, ldn, mch, brm;

        public TestObjectGraph() {
            liv = new TestCity("Liverpool", 12345);
            ldn = new TestCity("London", 678);
            mch = new TestCity("Manchester", 9012);
            brm = new TestCity("Birmingham", 8765);
            liv.setSisterCities(Lists.newArrayList(ldn, mch));
            mch.setSisterCities(Lists.newArrayList(liv));
            brm.setSisterCities(Lists.newArrayList(ldn));
            paul = new TestPerson("paul", 65, "nice guy");
            john = new TestPerson("john", 67, "dead guy");
            //TestPerson george = new TestPerson("george", 69, "one hit wonder");
            paul.setHomeTown(liv);
            paul.setNativeTown(brm);
            liv.setMayor(john);
        }
    }
    
    @Test
    public void testStoreLoadNestedRelationWithCyclesFullCascade() {
        TestObjectGraph g = new TestObjectGraph();
        
        assertNotLoaded(g.paul);
        assertNotLoaded(g.john);
        assertNotLoaded(g.liv);
        assertNotLoaded(g.ldn);
        assertNotLoaded(g.mch);
        assertNotLoaded(g.brm);
        
        ep.persist(g.paul);
        
        assertLoaded(g.paul);
        assertLoaded(g.john);
        assertLoaded(g.liv);
        assertLoaded(g.ldn);
        assertLoaded(g.mch);
        assertLoaded(g.brm);
        
        //white-box test to inspect the circular back reference from mch.ss[0] to liv
        JsonParser p = new JsonParser();
        JsonObject mchJson = p.parse(ep.findRawById(g.mch.getId(), TestCity.class)).getAsJsonObject();
        JsonObject mchSSref = mchJson.get("sisterCities").getAsJsonArray().get(0).getAsJsonObject();
        assertEquals(g.liv.getClass().getCanonicalName(), mchSSref.get("_ref_class").getAsString());
        assertEquals(g.liv.getId(), mchSSref.get("_ref_id").getAsString());
        
        TestCity mch2 = ep.findById(g.mch.getId(), TestCity.class, CascadeSpec.noCascade());
        TestCity mch2SS = mch2.getSisterCities().get(0);
        assertFalse(mch2SS.isLoaded());
        assertEquals(g.liv.getId(), mch2SS.getId());
        
        TestPerson paul2 = ep.findById(g.paul.getId(), TestPerson.class, CascadeSpec.cascade());
        assertEquals(g.paul, paul2);
        TestCity liv2 = paul2.getHomeTown();
        assertEquals(g.liv, liv2);
        assertEquals(g.john, liv2.getMayor());
        assertEquals(g.ldn, liv2.getSisterCities().get(0));
        assertEquals(g.mch, liv2.getSisterCities().get(1));
        assertTrue(liv2 == liv2.getSisterCities().get(1).getSisterCities().get(0));
        assertTrue(paul2.getNativeTown().getSisterCities().get(0) == liv2.getSisterCities().get(0)); //ldn
    }
    
    @Test
    public void testStoreNestedRelationWithCyclesWithCascadeSpec() {
        TestObjectGraph g = new TestObjectGraph();
        
        assertNotLoaded(g.paul);
        assertNotLoaded(g.john);
        assertNotLoaded(g.liv);
        assertNotLoaded(g.ldn);
        assertNotLoaded(g.mch);
        assertNotLoaded(g.brm);
        
        //persist paul, cascading into nativeTown (=brm) fully
        ep.persist(g.paul, CascadeSpec.noCascade().subCascade("nativeTown", CascadeSpec.cascade()));

        //=> only paul, brm (paul.nativeTown) and ldn (paul.nativeTown.sisterCities[0]) should've been persisted
        assertLoaded(g.paul);
        assertNotLoaded(g.john);
        assertNotLoaded(g.liv);
        assertLoaded(g.ldn);
        assertNotLoaded(g.mch);
        assertLoaded(g.brm);
        
        TestPerson paul2 = ep.findById(g.paul.getId(), TestPerson.class, CascadeSpec.cascade());
        assertEquals(g.paul, paul2);
        assertEquals(g.brm, paul2.getNativeTown());
        assertEquals(g.ldn, paul2.getNativeTown().getSisterCities().get(0));
        assertNull(paul2.getHomeTown());
    }

    @Test
    public void testStoreNestedRelationWithCyclesWithCascadeSpec2() {
        TestObjectGraph g = new TestObjectGraph();
        
        assertNotLoaded(g.paul);
        assertNotLoaded(g.john);
        assertNotLoaded(g.liv);
        assertNotLoaded(g.ldn);
        assertNotLoaded(g.mch);
        assertNotLoaded(g.brm);
        
        //persist paul, cascading into everything except nativeTown and homeTown.sisterCities[0]
        ep.persist(g.paul,
                CascadeSpec.cascade().subCascade("nativeTown", CascadeSpec.noCascade())
                                     .subCascade("homeTown", CascadeSpec.cascade()
                                                                        .subCascade("sisterCities\\[0\\]", CascadeSpec.noCascade())));

        //=> only paul, liv (paul.homeTown), john (paul.homeTown.mayor) and mch (paul.homeTown.sisterCities[1]) should've been persisted
        assertLoaded(g.paul);
        assertLoaded(g.john);
        assertLoaded(g.liv);
        assertNotLoaded(g.ldn);
        assertLoaded(g.mch);
        assertNotLoaded(g.brm);

        //load the whole graph as it was persisted, check it
        TestPerson paul2 = ep.findById(g.paul.getId(), TestPerson.class, CascadeSpec.cascade());
        assertEquals(g.paul, paul2);
        assertNull(paul2.getNativeTown());
        TestCity liv2 = paul2.getHomeTown();
        assertEquals(g.liv, liv2);
        assertEquals(g.john, liv2.getMayor());
        assertNull(liv2.getSisterCities().get(0));
        assertEquals(g.mch, liv2.getSisterCities().get(1));
        assertTrue(liv2 == liv2.getSisterCities().get(1).getSisterCities().get(0));

        //add the missing edges to the loaded graph, store it again
        paul2.setNativeTown(g.brm);
        g.brm.setSisterCities(Arrays.asList(g.ldn));
        liv2.getSisterCities().set(0, g.ldn);
        ep.persist(paul2, CascadeSpec.cascade());
        
        //now the in-db graph should be complete, i.e. equivalent to g
        TestPerson paul3 = ep.findById(paul2.getId(), TestPerson.class, CascadeSpec.cascade());
        assertEqualsIncludingId(paul2, paul3);
        assertEqualsIncludingId(g.brm, paul3.getNativeTown());
        assertEqualsIncludingId(g.ldn, paul3.getNativeTown().getSisterCities().get(0));
        assertEqualsIncludingId(g.liv, paul3.getHomeTown());
        assertEqualsIncludingId(g.john, paul3.getHomeTown().getMayor());
        assertEquals(Arrays.asList(g.ldn, g.mch), paul3.getHomeTown().getSisterCities());
        assertEqualsIncludingId(g.liv, paul3.getHomeTown().getSisterCities().get(1).getSisterCities().get(0));
    }

    @Test
    public void testLoadNestedRelationWithCyclesWithCascadeSpec() {
        TestObjectGraph g = new TestObjectGraph();
        
        assertNotLoaded(g.paul);
        assertNotLoaded(g.john);
        assertNotLoaded(g.liv);
        assertNotLoaded(g.ldn);
        assertNotLoaded(g.mch);
        assertNotLoaded(g.brm);
        
        //persist the entire graph
        ep.persist(g.paul, CascadeSpec.cascade());

        //=> everything should've been persisted
        assertLoaded(g.paul);
        assertLoaded(g.john);
        assertLoaded(g.liv);
        assertLoaded(g.ldn);
        assertLoaded(g.mch);
        assertLoaded(g.brm);
        
        TestPerson paul2 = ep.findById(g.paul.getId(), TestPerson.class,
                    CascadeSpec.cascade().subCascade("nativeTown", CascadeSpec.noCascade()));
        assertEquals(g.paul, paul2);
        TestCity liv2 = paul2.getHomeTown();
        assertEquals(g.liv, liv2);
        assertEquals(g.john, liv2.getMayor());
        assertEquals(g.ldn, liv2.getSisterCities().get(0));
        assertEquals(g.mch, liv2.getSisterCities().get(1));
        assertTrue(liv2 == liv2.getSisterCities().get(1).getSisterCities().get(0));
        assertIsStub(paul2.getNativeTown());
        assertFalse(g.brm.equals(paul2.getNativeTown()));
        assertEquals(g.brm.getId(), paul2.getNativeTown().getId());
        
        ep.load(paul2.getNativeTown(), CascadeSpec.cascade());
        assertLoaded(paul2.getNativeTown());
        assertEquals(g.brm, paul2.getNativeTown());
        assertEquals(g.ldn, paul2.getNativeTown().getSisterCities().get(0));
    }

    @Test
    public void testStoreLoadMultiple() {
        TestCity liv = new TestCity("Liverpool", 12345);
        TestPerson john = new TestPerson("john", 67, "foo");
        TestPerson paul = new TestPerson("paul", 65, "bar");
        TestPerson george = new TestPerson("george", 65, "baz");
        john.setHomeTown(liv);
        paul.setHomeTown(liv);
        george.setNativeTown(liv);
        
        ep.persist(CascadeSpec.cascade(), john, paul, george);

        assertLoaded(john);
        assertLoaded(paul);
        assertLoaded(george);
        assertLoaded(liv);

        Map<String, TestPerson> readback = ep.findById(TestPerson.class, CascadeSpec.cascade(), john.getId(), paul.getId(), george.getId());
        TestPerson john2 = readback.get(john.getId());
        TestPerson paul2 = readback.get(paul.getId());
        TestPerson george2 = readback.get(george.getId());

        assertEquals(john, john2);
        assertEquals(paul, paul2);
        assertEquals(george, george2);
        assertEquals(liv, john2.getHomeTown());
        assertTrue(john2.getHomeTown() == paul2.getHomeTown());
        assertTrue(john2.getHomeTown() == george2.getNativeTown());
        
        //check that the order of elements in readback corresponds to the passed IDs
        Iterator<TestPerson> it = readback.values().iterator();
        assertTrue(john2 == it.next());
        assertTrue(paul2 == it.next());
        assertTrue(george2 == it.next());
        assertFalse(it.hasNext());
    }

    @After
    public void tearDown() throws Exception {
        
    }
    
    @AfterClass
    public static void classTearDown() throws Exception {
        esClient.close();
        if (esRunner.isRunningLocally()) {
            esRunner.stopLocally();
            esRunner.cleanup();
        }
    }

}
