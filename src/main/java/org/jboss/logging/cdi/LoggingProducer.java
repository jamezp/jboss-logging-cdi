package org.jboss.logging.cdi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.logging.Logger;
import org.jboss.logging.Messages;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingProducer {

    @Produces
    public Logger produceLogger(final InjectionPoint injectionPoint) {
        return Logger.getLogger(getDeclaringType(injectionPoint));
    }

    // @Produces
    // @TypedMessageLogger
    public Object produceMessageLogger(final InjectionPoint injectionPoint) {
        final Class<?> type = getType(injectionPoint);
        return Logger.getMessageLogger(type, type.getName());
    }

    @Produces
    @LocalizedMessages
    public Object produceMessageBundle(final InjectionPoint injectionPoint) {
        final Annotated annotated = injectionPoint.getAnnotated();
        final Class<?> type = getType(injectionPoint);
        if (annotated.isAnnotationPresent(LocalizedMessages.class)) {
            final String locale = annotated.getAnnotation(LocalizedMessages.class).locale();
            if (!locale.trim().isEmpty()) {
                // TODO (jrp) parse string based locale
            }
        }
        return Messages.getBundle(type);
    }

    static Class<?> getDeclaringType(final InjectionPoint injectionPoint) {
        if (injectionPoint.getBean() != null) {
            return getRawType(injectionPoint.getBean().getBeanClass());
        }
        return getRawType(injectionPoint.getMember().getDeclaringClass());
    }

    static Class<?> getType(final InjectionPoint injectionPoint) {
        return getRawType(injectionPoint.getType());
    }

    @SuppressWarnings("unchecked")
    static <T> Class<T> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            if (((ParameterizedType) type).getRawType() instanceof Class<?>) {
                return (Class<T>) ((ParameterizedType) type).getRawType();
            }
        }
        return null;
    }
}
