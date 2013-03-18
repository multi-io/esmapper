package de.olafklischat.esmapper.json;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
			// TODO Auto-generated method stub
			return null;
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
		Gson gson = gsb.create();
		System.out.println(gson.toJson(p));
	}
	
}
