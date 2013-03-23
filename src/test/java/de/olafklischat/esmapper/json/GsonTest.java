package de.olafklischat.esmapper.json;

import java.io.IOException;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class GsonTest {
	
	private static class TAF implements TypeAdapterFactory {
		@SuppressWarnings("unchecked")
        @Override
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			System.out.println("creating adapter for " + type);
			if (TestOrg.class.equals(type.getType())) {
				return (TypeAdapter<T>) new OrgTA();
			}
			return null;
		}
	}

	private static class OrgTA extends TypeAdapter<TestOrg> {

		@Override
		public void write(JsonWriter out, TestOrg value)
				throws IOException {
			out.value(value.getName());
		}

		@Override
		public TestOrg read(JsonReader in) throws IOException {
            System.out.println("OrgTA#read");
            TestOrg result = new TestOrg();
            result.setName(in.nextString());
			return result;
		}
		
	}
	
	@SuppressWarnings("unused")
    private static class OrgSerDeser implements JsonSerializer<TestOrg>, JsonDeserializer<TestOrg> {
	    @Override
	    public JsonElement serialize(TestOrg src, Type typeOfSrc,
	            JsonSerializationContext context) {
	        return new JsonPrimitive(src.getName());
	    }
	    @Override
	    public TestOrg deserialize(JsonElement json, Type typeOfT,
	            JsonDeserializationContext context) throws JsonParseException {
	        TestOrg result = new TestOrg();
	        result.setName(json.getAsString());
	        return result;
	    }
	}

	public static void main(String[] args) throws Exception {
	    TestOrg org = new TestOrg();
		org.setName("BMW");
		org.setNrOfEmployees(123);
		TestProduct p = new TestProduct();
		p.setName("Z3");
		p.setProducer(org);
		
		GsonBuilder gsb = new GsonBuilder();
		gsb.setPrettyPrinting();
		gsb.registerTypeAdapterFactory(new TAF());
		//gsb.registerTypeAdapter(TestOrg.class, new OrgSerDeser());
		Gson gson = gsb.create();
		String json = gson.toJson(p);

		System.out.println(json);

		TestProduct p2 = gson.fromJson(json, TestProduct.class);
        System.out.println("readback: " + p2);
	}
	
}
