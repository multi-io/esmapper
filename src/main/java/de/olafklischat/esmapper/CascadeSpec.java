package de.olafklischat.esmapper;

import static org.elasticsearch.common.collect.Tuple.tuple;

import java.util.Deque;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.elasticsearch.common.collect.Tuple;

import com.google.common.base.Objects;

import de.olafklischat.esmapper.Entity;

/**
 * Specifies if and how an {@link EntityPersister#load(de.olafklischat.esmapper.Entity)
 * load} or {@link EntityPersister#persist(de.olafklischat.esmapper.Entity) persist}
 * operation for an {@link Entity} should cascade into other entities referenced
 * by that Entity. CascadeSpec is a recursive data structure, allowing you to
 * specify cascade/no-cascade policies for the referenced entities (and their
 * referenced entities, and so on) as well.
 * 
 * @author olaf
 */
public class CascadeSpec {
    
    private boolean defaultCascade;
    private final Deque<Tuple<Pattern, CascadeSpec>> subSpecsByPropPathPattern = new LinkedList<Tuple<Pattern,CascadeSpec>>();
    
    public CascadeSpec() {
        this(false);
    }
    
    public CascadeSpec(boolean cascade) {
        this.defaultCascade = cascade;
    }
    
    public static CascadeSpec cascade(boolean cascade) {
        return new CascadeSpec(cascade);
    }

    public static CascadeSpec cascade() {
        return new CascadeSpec(true);
    }

    public static CascadeSpec noCascade() {
        return new CascadeSpec(false);
    }

    /**
     * Cascade policy to be used by default, unless one was explicitly specified
     * for a property using {@link #subCascade(String, CascadeSpec)}
     * 
     * @return
     */
    public boolean isDefaultCascade() {
        return defaultCascade;
    }

    public void setCascade(boolean casecade) {
        this.defaultCascade = casecade;
    }
    
    public CascadeSpec subCascade(String propertyPathPattern, CascadeSpec ccs) {
        subSpecsByPropPathPattern.addFirst(tuple(Pattern.compile(propertyPathPattern), ccs));
        return this;
    }
    
    public CascadeSpec getSubSpecFor(String propertyPath) {
        for (Tuple<Pattern, CascadeSpec> patAndCcs : subSpecsByPropPathPattern) {
            if (patAndCcs.v1().matcher(propertyPath).matches()) {
                return patAndCcs.v2();
            }
        }
        return null;
    }

    public CascadeSpec getEffectiveSubSpecFor(String propertyPath) {
        return Objects.firstNonNull(getSubSpecFor(propertyPath), CascadeSpec.cascade(this.isDefaultCascade()));
    }

    /**
     * 
     * @param propertyName propertyName
     * @return the effective cascade policy for property propertyName
     */
    public boolean cascadesFor(String propertyPath) {
        return getEffectiveSubSpecFor(propertyPath).isDefaultCascade();
    }

}
