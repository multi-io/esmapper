package de.olafklischat.esmapper.json.stdimpl;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonUnmarshaller;
import de.olafklischat.esmapper.json.PropertyPath;

public class DefaultNumberUnmarshaller implements JsonUnmarshaller {

    @Override
    public boolean readJson(JsonReader r, PropertyPath targetPath,
            JsonConverter converter) throws IOException {
        if (r.peek() != JsonToken.NUMBER) {
            return false;
        }
        Number fromJson;
        try {
            fromJson = r.nextInt();
        } catch (NumberFormatException e) {
            try {
                fromJson = r.nextLong();
            } catch (NumberFormatException e2) {
                fromJson = r.nextDouble();
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
