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
        return Logger.getLogger(getCategory(injectionPoint));
    }

    @Produces
    @LocalizedLogger
    public Object produceMessageLogger(final InjectionPoint injectionPoint) {
        final String category = getCategory(injectionPoint);
        final Locale locale = getLocale(injectionPoint);
        return Logger.getMessageLogger(getRawType(injectionPoint.getType()), category, locale);
    }

    @Produces
    @LocalizedMessages
    public Object produceMessageBundle(final InjectionPoint injectionPoint) {
        final Locale locale = getLocale(injectionPoint);
        return Messages.getBundle(getRawType(injectionPoint.getType()), locale);
    }

    static <T> Class<T> getType(final InjectionPoint injectionPoint) {
        final Class<T> type;
        if (injectionPoint.getBean() != null) {
            type = getRawType(injectionPoint.getBean().getBeanClass());
        } else {
            type = getRawType(injectionPoint.getMember().getDeclaringClass());
        }
        return type;
    }

    @SuppressWarnings("unchecked")
    static <T> Class<T> getRawType(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            return getRawType(((ParameterizedType) type).getRawType());
        }
        return null;
    }

    private static String getCategory(final InjectionPoint injectionPoint) {
        final Annotated annotated = injectionPoint.getAnnotated();
        final Class<?> type = getType(injectionPoint);
        final StringBuilder category = new StringBuilder();
        if (annotated.isAnnotationPresent(Category.class)) {
            category.append(annotated.getAnnotation(Category.class).value());
        } else {
            category.append(type.getName());
        }
        if (annotated.isAnnotationPresent(Suffix.class)) {
            final String suffix = annotated.getAnnotation(Suffix.class).value();
            if (suffix != null) {
                category.append('.').append(suffix);
            }
        }
        return category.toString();
    }

    private Locale getLocale(final InjectionPoint injectionPoint) {
        final Annotated annotated = injectionPoint.getAnnotated();
        Locale result = Locale.getDefault();
        if (annotated.isAnnotationPresent(org.jboss.logging.cdi.Locale.class)) {
            final Class<? extends Resolver<Locale>> resolverClass = annotated.getAnnotation(org.jboss.logging.cdi.Locale.class).value();
            try {
                final Resolver<Locale> instance = resolverClass.newInstance();
                result = instance.resolve();
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return result;
    }
}
