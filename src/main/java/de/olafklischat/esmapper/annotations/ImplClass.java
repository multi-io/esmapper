package de.olafklischat.esmapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add to a property (its getter method, specifically) to tell the
 * JsonConverter which class to instantiate for that property when it needs to
 * be set during a JSON->Java conversion (deserialization). E.g. if the
 * property's type is java.util.List (which is an interface), any instantiatable
 * implementation class of List may be specified (if none is specified, some
 * reasonable default, e.g. ArrayList, is chosen heuristically, which may not
 * always be the desirable option).
 * 
 * @author olaf
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImplClass {
    Class<?> c();
    //TODO: ability to specify c'tor parameters too
}
