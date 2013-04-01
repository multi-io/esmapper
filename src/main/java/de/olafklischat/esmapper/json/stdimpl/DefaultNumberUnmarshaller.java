package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultNumberUnmarshaller implements JsonUnmarshaller {

    @Override
    public boolean readJson(JsonElement r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (! r.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive jsp = r.getAsJsonPrimitive();
        if (! jsp.isNumber()) {
            return false;
        }
        String jsonNum = jsp.toString();
        Number fromJson;
        try {
            fromJson = Integer.valueOf(jsonNum);
        } catch (NumberFormatException e) {
            try {
                fromJson = Long.valueOf(jsonNum);
            } catch (NumberFormatException e2) {
                fromJson = Double.valueOf(jsonNum);
            }
        }
        targetPath.set(convertNumber(fromJson, targetPath.getNodeClass()));
        return true;
    }

    private Object convertNumber(Number n, Class<?> targetType) {
        if (targetType.isPrimitive()) {
            //in this case, the JVM does the conversion
            return n;
        } else if (targetType == Long.class && (n instanceof Integer || n instanceof Long)) {
            return n.longValue();
        } else if (targetType == Float.class) {
            return n.floatValue();
        } else if (targetType == Double.class) {
            return n.doubleValue();
        } else {
            return n;
        }
    }

}
