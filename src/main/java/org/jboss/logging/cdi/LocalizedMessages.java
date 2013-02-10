package org.jboss.logging.cdi;

import static java.lang.annotation.ElementType.METHOD;
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
@Target(value = METHOD)
@Documented
@interface LocalizedMessages {
}
