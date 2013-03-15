package de.olafklischat.esmapper.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonConverter {

    private Gson gson = new Gson();
    
    public String toJson(Object o) {
        JsonElement jse = gson.toJsonTree(o);
        if (jse instanceof JsonObject) {
            JsonObject jsobj = (JsonObject) jse;
            jsobj.addProperty("_class", o.getClass().getCanonicalName());
        }
        return jse.toString();
    }
    
    public <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * Determines the class from required _class property in the JSON source.
     * 
     * @param json
     * @return
     */
    public Object fromJson(String json) {
        JsonParser p = new JsonParser();
        JsonElement jse = p.parse(json);
        if (jse instanceof JsonObject) {
            JsonObject jsobj = (JsonObject) jse;
            Class<?> clazz;
            try {
                clazz = Class.forName(jsobj.get("_class").getAsString());
            } catch (Exception e) {
                throw new IllegalArgumentException("couldn't determine class from JSON", e);
            }
            jsobj.remove("_class");
            return gson.fromJson(jsobj, clazz);
        } else {
            throw new IllegalArgumentException("couldn't determine class from JSON");
        }
    }

}
