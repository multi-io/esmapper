package de.olafklischat.esmapper.json;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonConverterTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testWriteJsonPrimitives() {
        JsonConverter c = new JsonConverter();
        assertEquals("42", c.toJson(42));
        assertEquals("-4223", c.toJson(-4223));
        assertEquals("12345678910", c.toJson(12345678910L));
        assertEquals("0", c.toJson(0));
        assertEquals("3.14", c.toJson(3.14));
        assertEquals("\"Hello World\"", c.toJson("Hello World"));
        assertEquals("true", c.toJson(true));
        assertEquals("false", c.toJson(false));
        assertEquals("null", c.toJson(null));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWriteJsonPrimitiveArray() {
        JsonConverter c = new JsonConverter();
        assertEquals("[42,23,-101]", c.toJson(new int[]{42,23,-101}));
        assertEquals("[42,-23,\"hello\"]", c.toJson(new Object[]{42,-23,"hello"}));
        assertEquals("[42,-23,\"hello\"]", c.toJson(Arrays.asList(42,-23,"hello")));
        assertEquals("[42,-23,98]", c.toJson(Ints.asList(42,-23,98)));
    }

    @Test
    public void testWriteJsonMap() {
        JsonConverter c = new JsonConverter();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", "Hubertus");
        m.put("age", 42);
        m.put("male", true);
        assertEquals("{\"_mapClass\":\"java.util.LinkedHashMap\",\"name\":\"Hubertus\",\"age\":42,\"male\":true}", c.toJson(m));
    }
    
    @Test
    public void testWriteJsonObject() throws Exception {
        JsonConverter c = new JsonConverter();
        TestOrg o = new TestOrg("Coca Cola Company", 125000, 3000);
        TestProduct p = new TestProduct("Coke", 299, new String[]{"milk","sugar"}, o);
        JsonObject jso = c.toJsonElement(p).getAsJsonObject();
        JsonObject jso2 = new JsonParser().parse(c.toJson(p)).getAsJsonObject();
        assertEquals(jso2, jso);
        assertEquals(p.getName(), jso.get("name").getAsString());
        assertEquals(p.getPrice(), jso.get("price").getAsInt());
        assertEquals(p.getPrice(), jso.get("price").getAsInt());
        List<String> pIngs = p.getIngredients();
        JsonArray jsIngs = jso.get("ingredients").getAsJsonArray();
        assertEquals(pIngs.size(), jsIngs.size());
        for (int i = 0; i < pIngs.size(); i++) {
            assertEquals(pIngs.get(i), jsIngs.get(i).getAsString());
        }
        assertEquals(p.getClass().getCanonicalName(), jso.get("_class").getAsString());
        assertEquals(5, jso.entrySet().size()); //name,price,ingredients,producer,_class

        JsonObject oJso = jso.get("producer").getAsJsonObject();

        assertEquals(o.getName(), oJso.get("name").getAsString());
        assertEquals(o.getNrOfEmployees(), oJso.get("nrOfEmployees").getAsInt());
        assertEquals(o.getRevenue(), oJso.get("revenue").getAsInt());
        assertEquals(4, oJso.entrySet().size()); //name,nrOfEmployees,revenue,_class
    }
    
    
    @Test
    public void testReadJsonPrimitives() {
        JsonConverter c = new JsonConverter();
        assertEquals(42, c.fromJson("42"));
        assertEquals(-4223, c.fromJson("-4223"));
        assertEquals(12345678910L, c.fromJson("12345678910"));
        assertEquals(0, c.fromJson("0"));
        assertEquals(3.14, c.fromJson("3.14"));
        assertEquals("Hello World", c.fromJson("\"Hello World\""));
        assertEquals(true, c.fromJson("true"));
        assertEquals(false, c.fromJson("false"));
        assertNull(c.fromJson("null"));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testReadJsonPrimitiveArray() {
        JsonConverter c = new JsonConverter();
        assertEquals(Ints.asList(42,23,-101), c.fromJson("[42,23,-101]"));
        assertEquals(Lists.newArrayList(42,-23,"hello"), c.fromJson("[42,-23,\"hello\"]"));
        assertEquals(Lists.newArrayList(42, true, false, null, "hello"), c.fromJson("[42,true,false,null,\"hello\"]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadJsonPrimitivesAndNestedArraysArray() {
        JsonConverter c = new JsonConverter();
        assertEquals(Lists.newArrayList(42, Lists.newArrayList(67, 89, true, "bla"), 23, -101, Lists.newArrayList(), "hello"),
                     c.fromJson("[42,[67,89,true,\"bla\"],23,-101,[],\"hello\"]"));
    }

    @Test
    public void testReadSimpleMap() {
        JsonConverter c = new JsonConverter();
        assertEquals(newMap("foo", "hello", "bar", 42), c.fromJson("{\"foo\":\"hello\", \"bar\":42}"));
        assertEquals(newMap("foo", "hello", "bar", 42, "baz", Lists.newArrayList(3,4,5)),
                     c.fromJson("{\"foo\":\"hello\", \"bar\":42, \"baz\":[3,4,5]}"));
        assertEquals(newMap("foo", "hello", "bar", true, "baz", null),
                     c.fromJson("{\"foo\":\"hello\", \"bar\":true, \"baz\":null}"));
    }

    @Test
    public void testReadMapWithClass() {
        JsonConverter c = new JsonConverter();
        Object res = c.fromJson("{_mapClass:\"java.util.LinkedHashMap\",foo:\"hello\", bar:42}");
        assertEquals(newMap("foo", "hello", "bar", 42), res);
        assertTrue(res instanceof LinkedHashMap<?, ?>);
    }
    
    @Test
    public void testReadSimpleObject() {
        JsonConverter c = new JsonConverter();
        assertEquals(new TestOrg("BMW", 120000, 35000),
                c.fromJson("{_class:\"de.olafklischat.esmapper.json.TestOrg\"," +
                           "name:\"BMW\", revenue:120000, nrOfEmployees: 35000}"));
    }
    
    @Test
    public void testReadNestedObject() {
        JsonConverter c = new JsonConverter();
        Object fromJson = c.fromJson("" +
                "{_class:\"de.olafklischat.esmapper.json.TestProduct\"," +
                "name: \"3er\"," +
                "price: 24000," +
                "ingredients: [\"engine\",\"wheels\"]," +
                "producer: " +
                    "{_class:\"de.olafklischat.esmapper.json.TestOrg\"," +
                    "name:\"BMW\"," +
                    "revenue:120000," +
                    "nrOfEmployees: 35000} }");
        assertEquals(new TestProduct("3er", 24000, new String[]{"engine","wheels"}, new TestOrg("BMW", 120000, 35000)), fromJson);
        assertTrue(((TestProduct)fromJson).getIngredients() instanceof ArrayList<?>);
    }
    
    @Test
    public void testReadAnnotatedObject() {
        JsonConverter c = new JsonConverter();
        TestCountry fromJson = (TestCountry) c.fromJson("" +
                "{_class:\"de.olafklischat.esmapper.json.TestCountry\"," +
                "name: \"Germany\"," +
                "population: 82000000," +
                "companies: [" +
                    "{_class:\"de.olafklischat.esmapper.json.TestOrg\"," +
                      "name:\"BMW\"," +
                      "revenue:120000," +
                      "nrOfEmployees: 35000}," +
                    "{_class:\"de.olafklischat.esmapper.json.TestOrg\"," +
                      "name:\"Mercedes\"," +
                      "revenue:456," +
                      "nrOfEmployees: 789} ]," +
                "ignored:\"ignoredValue\" }");
        TestOrg bmw = new TestOrg("BMW", 120000, 35000);
        TestOrg merc = new TestOrg("Mercedes", 456, 789);
        TestCountry ger = new TestCountry("Germany", 82000000, Lists.newArrayList(bmw, merc), null);
        assertEquals(ger, fromJson);
        assertNull(fromJson.getIgnored()); //should've been caught by the previous test, but check explicitly again
        assertTrue(fromJson.getCompanies() instanceof LinkedList<?>);
    }
    
    @Test
    public void testReadWithConversions() {
        String json = "" +
        		"{_class:\"de.olafklischat.esmapper.json.TestAutoConversionsBean\"," +
        		"primInt:1," +
        		"primLong:2," +
                "primFloat:3," +
                "primDouble:4," +
        		"objInt:5," +
        		"objLong:6," +
                "objFloat:7," +
                "objDouble:8}";
        TestAutoConversionsBean expected = new TestAutoConversionsBean(1, 2, 3, 4, 5, 6L, 7F, 8D);
        JsonConverter c = new JsonConverter();
        assertEquals(expected, c.fromJson(json));
        
        try {
            c.fromJson("{_class:\"de.olafklischat.esmapper.json.TestAutoConversionsBean\",primInt:1.7}");
            fail("exception expected");
        } catch (IllegalStateException e) {
        }
        try {
            c.fromJson("{_class:\"de.olafklischat.esmapper.json.TestAutoConversionsBean\",objInt:1.7}");
            fail("exception expected");
        } catch (IllegalStateException e) {
        }
        try {
            c.fromJson("{_class:\"de.olafklischat.esmapper.json.TestAutoConversionsBean\",objLong:1.7}");
            fail("exception expected");
        } catch (IllegalStateException e) {
        }
    }

    private static Map<?, ?> newMap(Object... keysAndValues) {
        Map<Object,Object> result = new HashMap<Object, Object>();
        boolean onKey = true;
        Object currKey = null;
        for(Object kv : keysAndValues) {
            if (onKey) {
                currKey = kv;
            } else {
                result.put(currKey, kv);
            }
            onKey = !onKey;
        }
        return result;
    }

}

