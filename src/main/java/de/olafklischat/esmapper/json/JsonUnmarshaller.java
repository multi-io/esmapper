package de.olafklischat.esmapper.json;

import java.io.IOException;

import com.google.gson.JsonElement;

import de.olafklischat.esmapper.json.stdimpl.DefaultArrayUnmarshaller;
import de.olafklischat.esmapper.json.stdimpl.DefaultObjectUnmarshaller;

/**
 * Base interface for JSON unmarshallers, which perform the actual grunt work of converting
 * a JSON tree into a Java object tree during a {@link JsonConverter}.fromJson* / .readJson call.
 * <p>
 * Unmarshallers must be registered with {@link JsonConverter} instances via
 * {@link JsonConverter#registerUnmarshaller(JsonUnmarshaller)}. They are invoked for every
 * subtree (node) of the input JSON graph as it needs to be written to the corresponding
 * output subtree of the output Java object.
 * <p>
 * JsonConverter itself already registers default unmarshallers to support unmarshalling
 * all the common kinds of JSON nodes, i.e. primitives, arrays and objects.
 * See the impl subpackage for their implementations.
 * 
 * @author Olaf Klischat
 */
public interface JsonUnmarshaller {
    /**
     * Perform an actual JSON->Java subtree conversion, or return false if this unmarshaller
     * can't handle the particular input node and wants to delegate the work to the next
     * registered unmarshaller.
     * 
     * @param source the JSON subtree (node) to be unmarshalled.
     * @param targetPath path to the Java subtree that the result of the conversion should be written to.
     *        The path is rooted at the output object that's being created and will be returned by
     *        the JsonConverter fromJson* / readJson* method at the end of the operation.
     * @param context the JsonConverter that's doing this operation. This is particularly needed
     *        for unmarshallers that can't unmarshal their entire JSON tree themselves, but
     *        need to delegate the unmarshalling of some subtrees of it back to the JsonConverter.
     *        This would be the case, for example, for the elements of an array
     *        (see e.g. {@link DefaultArrayUnmarshaller}) or for the values of an object
     *        (see e.g. {@link DefaultObjectUnmarshaller}). For each of
     *        those subtrees, the unmarshaller should construct a corresponding target PropertyPath that
     *        contains the entire path from the root output object to that sub-object, and then call
     *        {@link JsonConverter#readJson(JsonElement, PropertyPath)} with it and the corresponding
     *        JSON sub-element of source. See the {@link DefaultArrayUnmarshaller} and
     *        and {@link DefaultObjectUnmarshaller} for some common implementations of this pattern.
     * @return true to signal that the unmarshaller performed its work and the source object
     *         has been converted and written to targetPath. false if the unmarshaller didn't perform its work,
     *         in which case the unmarshaller must not have written anything to the target. This is common: Unmarshallers will
     *         inspect source and/or targetPath and decide whether or not
     *         they can unmarshal that type of JSON tree or not. For example, the {@link DefaultArrayUnmarshaller}
     *         will only unmarshal JSON arrays, but not e.g. strings or numbers (remember that
     *         JsonConverter will invoke all unmarshallers for all nodes of the input JSON graph, so
     *         every registered unmarshaller will "see" all those nodes)
     * @throws if the unmarshaller determined that there was a semantic error in the input or output,
     *         e.g. if the unmarshaller found that the JSON type of source doesn't match the Java type of targetPath
     *         (the latter may be determined using targetPath.getHead().getType()).
     *         That may be the case e.g. if an array unmarshaller was trying to write a JSON array into
     *         a Java string property.
     * 
     * @param source
     * @param targetPath
     * @param context
     * @return
     * @throws IOException
     */
    boolean readJson(JsonElement source, PropertyPath targetPath, JsonConverter context) throws IOException;
}
