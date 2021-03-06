package de.olafklischat.esmapper.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

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
    public void testReadSimpleObjectWithClassHint() {
        JsonConverter c = new JsonConverter();
        assertEquals(new TestOrg("BMW", 120000, 35000),
                c.fromJson("{name:\"BMW\", revenue:120000, nrOfEmployees: 35000}", TestOrg.class));
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
    public void testWriteAnnotatedObject() {
        JsonConverter c = new JsonConverter();
        TestOrg bmw = new TestOrg("BMW", 120000, 35000);
        TestOrg merc = new TestOrg("Mercedes", 456, 789);
        TestCountry ger = new TestCountry("Germany", 82000000, Lists.newArrayList(bmw, merc), null);
        ger.setIgnored("ignoreme");
        assertEquals("ignoreme", ger.getIgnored());
        JsonObject jso = c.toJsonElement(ger).getAsJsonObject();
        assertEquals("Germany", jso.get("name").getAsString());
        assertEquals(82000000, jso.get("population").getAsInt());
        assertNull(jso.get("ignored"));
        assertEquals(42, jso.get("readOnly42").getAsInt());
    }

    @Test
    public void testReadIntoIgnoredPropertyDoesntChangeItsValue() {
        String json = "{_class:\"de.olafklischat.esmapper.json.TestCountry\"," +
                "name: \"Germany\"," +
                "population: 82000000," +
                "companies: null," +
                "ignored:\"newIgnoredValue\" }";
        TestCountry target = new TestCountry("defaultName", 123, null, "defaultIgnoredValue");
        JsonConverter c = new JsonConverter();
        c.readJson(json, target);
        TestCountry ger = new TestCountry("Germany", 82000000, null, "defaultIgnoredValue");
        assertEquals(ger, target);
        assertEquals("defaultIgnoredValue", target.getIgnored()); //should've been caught by the previous test, but check explicitly again
    }

    @Test
    public void testReadIntoReadOnlyProperty() {
        JsonConverter c = new JsonConverter();
        String json = "{_class:\"de.olafklischat.esmapper.json.TestCountry\"," +
                "name: \"Germany\"," +
                "population: 82000000," +
                "companies: null," +
                "readOnly42: 23," +
                "ignored:\"ignoredValue\" }";
        try {
            TestCountry fromJson = (TestCountry) c.fromJson(json);
            TestCountry ger = new TestCountry("Germany", 82000000, null, null);
            assertEquals(ger, fromJson);
            fail("IllegalStateException (property not writable) expected");
        } catch (IllegalStateException e) {
            //expected exception. Trying this (having a property in the input JSON
            //  that's only readable in Java) is an error atm. May change/be configurable in the future.
        }
    }
    
    @Test
    public void testReadIntoIgnoredReadOnlyProperty() {
        JsonConverter c = new JsonConverter();
        String json = "{_class:\"de.olafklischat.esmapper.json.TestCountry\"," +
                "name: \"Germany\"," +
                "population: 82000000," +
                "companies: null," +
                "ignoredReadOnly23: 42 }";
        TestCountry fromJson = (TestCountry) c.fromJson(json);
        TestCountry ger = new TestCountry("Germany", 82000000, null, null);
        assertEquals(ger, fromJson);
    }

    @Test
    public void testReadIntoPreExistingObject() {
        JsonConverter c = new JsonConverter();
        TestCountry target = new TestCountry("Foo", 1234, null, null);
        target.setIgnored("ignoreme");
        c.readJson("" +
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
                "ignored:\"ignoredValue\" }",
                target);
        TestOrg bmw = new TestOrg("BMW", 120000, 35000);
        TestOrg merc = new TestOrg("Mercedes", 456, 789);
        TestCountry ger = new TestCountry("Germany", 82000000, Lists.newArrayList(bmw, merc), null);
        ger.setIgnored("ignoreme");
        assertEquals(ger, target);
        assertEquals("ignoreme", target.getIgnored()); //should've been caught by the previous test, but check explicitly again
        assertTrue(target.getCompanies() instanceof LinkedList<?>);
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
    
    @Test
    public void testCustomMarshallerUnmarshaller() {
        String json = "" +
        		"{name:\"A-Class\"," +
        		"price:123," +
        		"ingredients:[\"engine\",\"wheels\"]," +
        		"producer:\"Mercedes,456,789\"}";
        JsonConverter c = new JsonConverter();

        //deserialization with custom unmarshaller
        c.registerUnmarshaller(new TestOrgFromStringUnmarshaller());
        TestProduct p = c.fromJson(json, TestProduct.class);
        assertEquals("A-Class", p.getName());
        assertEquals(123, p.getPrice());
        assertEquals(Lists.newArrayList("engine", "wheels"), p.getIngredients());
        assertEquals(new TestOrg("Mercedes", 456, 789), p.getProducer());
        
        p.getProducer().setName("DaimlerChrysler");
        p.getProducer().setNrOfEmployees(999);

        //serialization without custom marshaller
        JsonObject pJso1 = c.toJsonElement(p).getAsJsonObject();
        assertEquals("A-Class", pJso1.get("name").getAsString());
        JsonObject oJso1 = pJso1.get("producer").getAsJsonObject();
        assertEquals("DaimlerChrysler", oJso1.get("name").getAsString());
        assertEquals(456, oJso1.get("revenue").getAsInt());
        assertEquals(999, oJso1.get("nrOfEmployees").getAsInt());

        //serialization with custom marshaller
        c.registerMarshaller(new TestOrgToStringMarshaller());
        JsonObject pJso2 = c.toJsonElement(p).getAsJsonObject();
        assertEquals("A-Class", pJso2.get("name").getAsString());
        assertEquals("DaimlerChrysler,456,999", pJso2.get("producer").getAsString());
    }

    private static class TestOrgToStringMarshaller implements JsonMarshaller {
        @Override
        public boolean writeJson(PropertyPath sourcePath, JsonWriter out,
                JsonConverter context) throws IOException {
            if (sourcePath.getNodeClass() != TestOrg.class) {
                return false;
            }
            TestOrg org = (TestOrg) sourcePath.get();
            out.value(org.getName() + "," + org.getRevenue() + "," + org.getNrOfEmployees());
            return true;
        }
    }
    
    private static class TestOrgFromStringUnmarshaller implements JsonUnmarshaller {
        @Override
        public boolean readJson(JsonElement r, PropertyPath targetPath,
                JsonConverter context) throws IOException {
            if (targetPath.getNodeClass() != TestOrg.class) {
                return false;
            }
            String orgStr = r.getAsString();
            StringTokenizer st = new StringTokenizer(orgStr, ",");
            TestOrg org = new TestOrg(st.nextToken(), Integer.valueOf(st.nextToken()), Integer.valueOf(st.nextToken()));
            targetPath.set(org);
            return true;
        }
    }

    @Test
    public void testPropertyPaths() {
        String json = "" +
                "{name:\"A-Class\"," +
                 "price:123," +
                 "ingredients:[\"engine\",\"wheels\"]," +
                 "producer:{" +
                     "name:\"Mercedes\"," +
                     "revenue:456," +
                     "nrOfEmployees:789" +
                 "}" +
                "}";
        JsonConverter c = new JsonConverter();

        PathTrackingMarshallerUnmarshaller um = new PathTrackingMarshallerUnmarshaller();
        c.registerUnmarshaller(um);
        TestProduct p = c.fromJson(json, TestProduct.class);
        assertEquals("A-Class", p.getName());
        assertEquals(123, p.getPrice());
        assertEquals(Lists.newArrayList("engine", "wheels"), p.getIngredients());
        assertEquals(new TestOrg("Mercedes", 456, 789), p.getProducer());
        assertTrue(um.seenPathNames.contains("name"));
        assertTrue(um.seenPathNames.contains("price"));
        assertTrue(um.seenPathNames.contains("ingredients"));
        assertTrue(um.seenPathNames.contains("ingredients[0]"));
        assertTrue(um.seenPathNames.contains("ingredients[1]"));
        assertTrue(um.seenPathNames.contains("producer"));
        assertTrue(um.seenPathNames.contains("producer.name"));
        assertTrue(um.seenPathNames.contains("producer.nrOfEmployees"));
        assertTrue(um.seenPathNames.contains("producer.revenue"));
        
        PathTrackingMarshallerUnmarshaller m = new PathTrackingMarshallerUnmarshaller();
        c.registerMarshaller(m);
        JsonObject pJso2 = c.toJsonElement(p).getAsJsonObject();
        assertEquals("A-Class", pJso2.get("name").getAsString());
        assertTrue(m.seenPathNames.contains("name"));
        assertTrue(m.seenPathNames.contains("price"));
        assertTrue(m.seenPathNames.contains("ingredients"));
        assertTrue(m.seenPathNames.contains("ingredients[0]"));
        assertTrue(m.seenPathNames.contains("ingredients[1]"));
        assertTrue(m.seenPathNames.contains("producer"));
        assertTrue(m.seenPathNames.contains("producer.name"));
        assertTrue(m.seenPathNames.contains("producer.nrOfEmployees"));
        assertTrue(m.seenPathNames.contains("producer.revenue"));
    }

    private static class PathTrackingMarshallerUnmarshaller implements JsonMarshaller, JsonUnmarshaller {
        private final List<String> seenPathNames = new ArrayList<String>();
        @Override
        public boolean writeJson(PropertyPath sourcePath, JsonWriter out,
                JsonConverter context) throws IOException {
            seenPathNames.add(sourcePath.getPathNotation());
            return false;
        }
        @Override
        public boolean readJson(JsonElement r, PropertyPath targetPath,
                JsonConverter context) throws IOException {
            seenPathNames.add(targetPath.getPathNotation());
            return false;
        }
    }
    
    @Test
    public void testCustomMarshallingFilter() {
        TestProduct p = new TestProduct("iPhone", 999, new String[]{"CPU", "screen"}, null);
        
        JsonConverter c = new JsonConverter();
        JsonObject json = c.toJsonElement(p).getAsJsonObject();
        assertEquals("iPhone", json.get("name").getAsString());
        assertEquals(999, json.get("price").getAsInt());
        
        JsonMarshallingFilter nameFilter = new JsonMarshallingFilter() {
            @Override
            public boolean shouldMarshal(PropertyPath sourcePath, JsonConverter context) {
                return !(sourcePath.getHead().isPropertyAccess() && "name".equals(sourcePath.getHead().getPropDescriptor().getName()));
            }
        };
        c.registerMarshallingFilter(nameFilter);
        json = c.toJsonElement(p).getAsJsonObject();
        assertNull("expecting the name property to be filtered out", json.getAsJsonObject().get("name"));
        assertEquals(999, json.getAsJsonObject().get("price").getAsInt());

        NumbersHolder ns = new NumbersHolder(20, 70, 35, new int[]{-3,5,6,72,49,58,23,123,42});
        JsonMarshallingFilter numberFilter = new JsonMarshallingFilter() {
            @Override
            public boolean shouldMarshal(PropertyPath sourcePath, JsonConverter context) {
                Object inputValue = sourcePath.get();
                if (!(inputValue instanceof Number)) {
                    return true;
                }
                int n = (int) inputValue;
                return n < 50;
            }
        };
        c = new JsonConverter();
        c.registerMarshallingFilter(numberFilter);
        json = c.toJsonElement(ns).getAsJsonObject();
        assertEquals(20, json.get("x").getAsInt());
        assertNull("int value expected to be filtered out", json.get("y"));
        assertEquals(35, json.get("z").getAsInt());
        Iterator<JsonElement> numbersIt = json.get("numbers").getAsJsonArray().iterator();
        assertEquals(-3, numbersIt.next().getAsInt());
        assertEquals(5, numbersIt.next().getAsInt());
        assertEquals(6, numbersIt.next().getAsInt());
        assertEquals(49, numbersIt.next().getAsInt());
        assertEquals(23, numbersIt.next().getAsInt());
        assertEquals(42, numbersIt.next().getAsInt());
        assertTrue(!numbersIt.hasNext());
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

