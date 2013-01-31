package org.jboss.logging.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Qualifier
@Retention(RUNTIME)
@Target(value = {TYPE, METHOD, FIELD, PARAMETER})
@Documented
public @interface LocalizedMessages {

    /**
     * The local used for the message bundle.
     *
     * @return the local
     */
    String locale() default "";
}
