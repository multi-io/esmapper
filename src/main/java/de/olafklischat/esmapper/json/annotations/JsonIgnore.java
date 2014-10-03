package de.olafklischat.esmapper.json.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add to a property (its getter method, specifically) to tell the
 * JsonConverter to ignore that property during JSON serialization and
 * deserialization.
 * 
 * @author olaf
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnore {
}
