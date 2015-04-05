package de.olafklischat.esmapper.json.stdimpl;

import de.olafklischat.esmapper.json.JsonConverter;
import de.olafklischat.esmapper.json.JsonMarshallingFilter;
import de.olafklischat.esmapper.json.PropertyPath;
import de.olafklischat.esmapper.json.annotations.JsonIgnore;

public class JsonIgnoreMarshallingFilter implements JsonMarshallingFilter {

    @Override
    public boolean shouldMarshal(PropertyPath sourcePath, JsonConverter context) {
        return null == sourcePath.getAnnotation(JsonIgnore.class);
    }

}
