package de.olafklischat.esmapper.json;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.primitives.Ints;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
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
        TestProduct p = new TestProduct("Coke", 299, new String[]{"milk","sugar"}, null);
        JsonObject jso = c.toJsonElement(p).getAsJsonObject();
        JsonObject jso2 = new JsonParser().parse(c.toJson(p)).getAsJsonObject();
        assertEquals(jso2, jso);
        assertEquals(p.getName(), jso.get("name").getAsString());
        assertEquals(p.getPrice(), jso.get("price").getAsInt());
        assertEquals(p.getPrice(), jso.get("price").getAsInt());
        String[] pIngs = p.getIngredients();
        JsonArray jsIngs = jso.get("ingredients").getAsJsonArray();
        assertEquals(pIngs.length, jsIngs.size());
        for (int i = 0; i < pIngs.length; i++) {
            assertEquals(pIngs[i], jsIngs.get(i).getAsString());
        }
        assertEquals(JsonNull.INSTANCE, jso.get("producer"));
        assertEquals(p.getClass().getCanonicalName(), jso.get("_class").getAsString());
        assertEquals(5, jso.entrySet().size()); //name,price,ingredients,producer,_class
    }

}

