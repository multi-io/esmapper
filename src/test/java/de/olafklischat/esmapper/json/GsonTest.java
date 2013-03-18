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

import de.olafklischat.esmapper.Organization;
import de.olafklischat.esmapper.Product;

public class GsonTest {
	
	private static class TAF implements TypeAdapterFactory {
		@Override
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			System.out.println("creating adapter for " + type);
			if (Organization.class.equals(type.getType())) {
				return (TypeAdapter<T>) new OrgTA();
			}
			return null;
		}
	}

	private static class OrgTA extends TypeAdapter<Organization> {

		@Override
		public void write(JsonWriter out, Organization value)
				throws IOException {
			out.value(value.getName());
		}

		@Override
		public Organization read(JsonReader in) throws IOException {
            System.out.println("OrgTA#read");
            Organization result = new Organization();
            result.setName(in.nextString());
			return result;
		}
		
	}
	
	private static class OrgSerDeser implements JsonSerializer<Organization>, JsonDeserializer<Organization> {
	    @Override
	    public JsonElement serialize(Organization src, Type typeOfSrc,
	            JsonSerializationContext context) {
	        return new JsonPrimitive(src.getName());
	    }
	    @Override
	    public Organization deserialize(JsonElement json, Type typeOfT,
	            JsonDeserializationContext context) throws JsonParseException {
	        Organization result = new Organization();
	        result.setName(json.getAsString());
	        return result;
	    }
	}

	public static void main(String[] args) throws Exception {
		Organization org = new Organization();
		org.setId("123");
		org.setName("BMW");
		Product p = new Product();
		p.setId("456");
		p.setName("Z3");
		p.setProducer(org);
		
		GsonBuilder gsb = new GsonBuilder();
		gsb.setPrettyPrinting();
		gsb.registerTypeAdapterFactory(new TAF());
		//gsb.registerTypeAdapter(Organization.class, new OrgSerDeser());
		Gson gson = gsb.create();
		String json = gson.toJson(p);

		System.out.println(json);

		Product p2 = gson.fromJson(json, Product.class);
        System.out.println("readback: " + p2);
	}
	
}
