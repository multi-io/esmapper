package de.olafklischat.esmapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that specifies a boolean "loaded" property of entities.
 * If defined, EntityPersister will set this flag to true for entities which
 * have been fully loaded from ES. (this won't be the case for all entities
 * with a non-null id -- some of those may be stubs).
 * 
 * @author olaf
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoadedFlag {
}
