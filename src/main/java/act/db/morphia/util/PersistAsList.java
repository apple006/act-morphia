package act.db.morphia.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a {@link org.osgl.util.KVStore} typed field indicate the
 * data should be persisted as a list of {Key, Value} pairs instead
 * of an Object map
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface PersistAsList {
}
