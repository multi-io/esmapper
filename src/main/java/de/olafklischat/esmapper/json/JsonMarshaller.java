package de.olafklischat.esmapper.json;

import java.io.IOException;

import com.google.gson.stream.JsonWriter;

import de.olafklischat.esmapper.json.stdimpl.DefaultArrayMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultCollectionMarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultMapMarshaller;

/**
 * Base interface for JSON marshallers, which perform the actual grunt work of converting
 * a Java object graph tree into a JSON tree during a {@link JsonConverter}.toJson* / .writeJson call.
 * <p>
 * Marshallers must be registered with {@link JsonConverter} instances via
 * {@link JsonConverter#registerMarshaller(JsonMarshaller)}. They are invoked for every
 * subtree (node) of the input Java object graph as it needs to be written to the corresponding
 * output subtree of the JSON output stream.
 * <p>
 * JsonConverter itself already registers default marshallers to support marshalling
 * all the common kinds of Java objects, e.g. primitives, objects, arrays, collections,
 * and maps. See the impl subpackage for their implementations.
 * 
 * @author Olaf Klischat
 *
 */
public interface JsonMarshaller {
    /**
     * Perform an actual Java->JSON subtree conversion, or return false if this marshaller
     * can't handle the particular input node and wants to delegate the work to the next
     * registered marshaller.
     * 
     * @param sourcePath path to the Java object subtree (node) to be marshalled. The path
     *        is rooted at the input object passed to the JsonConverter toJson* / writeJson* method.
     * @param out the JSON output stream the object should be written to
     * @param context the JsonConverter that's doing this operation. This is particularly needed
     *        for marshallers that can't marshal their entire object tree themselves, but
     *        need to delegate the marshalling of some subtrees of it back to the JsonConverter.
     *        This would be the case, for example, for the array/list elements of an array or list
     *        (see e.g. {@link DefaultArrayMarshaller}) or for the values of a map. For each of
     *        those subtrees, the marshaller should construct a corresponding PropertyPath that
     *        contains the entire path from the root to the subtree, and then call
     *        {@link JsonConverter#writeJson(PropertyPath, JsonWriter)} with it. See the {@link DefaultArrayMarshaller},
     *        {@link DefaultCollectionMarshaller} and {@link DefaultMapMarshaller} for some common
     *        implementations of this pattern.
     * @return true to signal that the marshaller performed its work and the object tree below
     *         sourcePath has been marshalled. false if the marshaller didn't perform its work,
     *         in which case the marshaller must not have written anything to out. This is common:
     *         Marshallers will inspect sourcePath and/or the Java subtree below it and decide whether or not
     *         they can marshal that type of Java object or not. For example, the {@link DefaultCollectionMarshaller}
     *         will only marshal Java collections, but not e.g. strings or numbers (remember that
     *         JsonConverter will invoke all marshallers for all nodes of the input object graph, so
     *         every registered marshaller will "see" all those nodes)
     * @throws IOException
     */
    boolean writeJson(PropertyPath sourcePath, JsonWriter out, JsonConverter context) throws IOException;
}
