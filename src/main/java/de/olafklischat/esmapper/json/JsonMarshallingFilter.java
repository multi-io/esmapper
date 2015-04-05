package de.olafklischat.esmapper.json;

import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.stdimpl.DefaultMapMarshaller;

/**
 * Special interface for filters that are called by during marshalling to determine whether
 * a given subtree of the input Java object should be marshalled or not.
 * <p>
 * JsonMarshallingFilters must be registered via {@link JsonConverter#registerMarshallingFilter(JsonMarshallingFilter)}.
 * During marshalling, a {@link JsonMarshaller marshaller} may call the registered filters
 * (via {@link JsonConverter#shouldMarshal(PropertyPath)}) to determine whether a given
 * subtree of its input Java tree (e.g. a value of a map or an element of a list) should be passed to
 * {@link JsonConverter#writeJson(PropertyPath, com.google.gson.stream.JsonWriter)} to be marshalled
 * itself via the registered marshallers.
 * <p>
 * In most cases, this would not be necessary because one of the registered marshallers
 * may itself implement the filter check and decide to just marshal nothing
 * (and return true from {@link JsonMarshaller#writeJson(PropertyPath, com.google.gson.stream.JsonWriter, JsonConverter)}
 * to indicate completion) in case the filter criteria (whatever it may be) doesn't
 * hold true. But there are cases in which this simple approach wouldn't work. The most common
 * one is map marshalling ({@link DefaultMapMarshaller}): When marshalling a key/value
 * pair of the input map, the marshaller would first write the key (a string) to the
 * JSON output, followed by a colon, then call {@link JsonConverter#writeJson(PropertyPath, com.google.gson.stream.JsonWriter)}
 * to marshal the value. If that writeJson call just wrote nothing in this situation,
 * an invalid JSON output would result (a key string, followed by a colon, followed immediately
 * by the next key or the end of the object), which would raise an exception and abort
 * the whole operation. So what the map marshaller must do is call {@link JsonConverter#shouldMarshal(PropertyPath)}
 * before writing the key, and if that returns false, skip the entire key/value pair.
 * <p>
 * For consistency and predictability, not just map marshallers, but all marshallers that have subtrees
 * in their input that they want to pass to {@link JsonConverter#writeJson(PropertyPath, JsonWriter)}
 * should first determine whether or not to do that by calling {@link JsonConverter#shouldMarshal(PropertyPath)}.
 * 
 * @author Olaf Klischat
 */
public interface JsonMarshallingFilter {
    boolean shouldMarshal(PropertyPath sourcePath, JsonConverter context);
}
