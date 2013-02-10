/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.logging.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Named;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingExtension implements Extension {
    static final Logger LOGGER = Logger.getLogger(LoggingExtension.class);
    static final AnnotationLiteral<Any> ANY = new AnnotationLiteral<Any>() {
    };
    static final AnnotationLiteral<Default> DEFAULT = new AnnotationLiteral<Default>() {
    };
    static final AnnotationLiteral<MessageBundle> MESSAGE_BUNDLE = new AnnotationLiteral<MessageBundle>() {
    };
    static final AnnotationLiteral<MessageLogger> MESSAGE_LOGGER = new AnnotationLiteral<MessageLogger>() {
    };

    private final List<AnnotatedType<Object>> messageBundles = new ArrayList<AnnotatedType<Object>>();
    private final List<AnnotatedType<Object>> messageLoggers = new ArrayList<AnnotatedType<Object>>();
    private Bean<Object> messageProducerBean = null;
    private Bean<Object> loggerProducerBean = null;


    void processAnnotatedType(@Observes ProcessAnnotatedType<Object> pat) {
        final AnnotatedType<Object> annotatedType = pat.getAnnotatedType();
        if (annotatedType.isAnnotationPresent(InjectableMessageBundle.class)) {
            messageBundles.add(annotatedType);
        }
        if (annotatedType.isAnnotationPresent(InjectableMessageLogger.class)) {
            messageLoggers.add(annotatedType);
        }
    }

    void processProducerMethod(@Observes ProcessProducerMethod<Object, LoggingProducer> producer) {
        if (producer.getAnnotatedProducerMethod().isAnnotationPresent(LocalizedLogger.class)) {
            loggerProducerBean = cast(producer.getBean());
        }
        if (producer.getAnnotatedProducerMethod().isAnnotationPresent(LocalizedMessages.class)) {
            messageProducerBean = cast(producer.getBean());
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        for (final AnnotatedType<?> annotatedType : messageBundles) {
            if (messageProducerBean != null) {
                // event.addBean(createBean(messageProducerBean, annotatedType, beanManager));
                event.addBean(createBean(messageProducerBean, annotatedType, beanManager, MESSAGE_BUNDLE));
            } else {
                // TODO (jrp) possibly throw exception
                LOGGER.warnf("Could not add bean for producing @%s. No default producer found.", InjectableMessageBundle.class.getName());
            }
        }
        for (final AnnotatedType<?> annotatedType : messageLoggers) {
            if (loggerProducerBean != null) {
                // event.addBean(createBean(loggerProducerBean, annotatedType, beanManager, LocalizedLiteral.INSTANCE));
                event.addBean(createBean(loggerProducerBean, annotatedType, beanManager, MESSAGE_LOGGER));
            } else {
                // TODO (jrp) possibly throw exception
                LOGGER.warnf("Could not add bean for producing @%s. No default producer found.", InjectableMessageLogger.class.getName());
            }
        }
    }

    void cleanUp(@Observes AfterDeploymentValidation event) {
        messageBundles.clear();
        messageLoggers.clear();
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(final Object obj) {
        return (T) obj;
    }

    static <T> Bean<T> createBean(final Bean<Object> delegate, final AnnotatedType<T> type, final BeanManager beanManager, final Annotation... qualifiers) {
        final Set<Type> types = new HashSet<Type>(type.getTypeClosure());
        final Set<Annotation> beanQualifiers = new HashSet<Annotation>(Arrays.asList(qualifiers));
        final Set<Class<? extends Annotation>> stereotypes = new HashSet<Class<? extends Annotation>>();
        String name = null;
        Class<? extends Annotation> scope = Dependent.class;
        for (Annotation annotation : type.getAnnotations()) {
            if (beanManager.isQualifier(annotation.annotationType())) {
                beanQualifiers.add(annotation);
            } else if (annotation.annotationType().equals(Named.class)) {
                name = Named.class.cast(annotation).value();
            } else if (beanManager.isScope(annotation.annotationType())) {
                scope = annotation.annotationType();
            } else if (beanManager.isStereotype(annotation.annotationType())) {
                stereotypes.add(annotation.annotationType());
            }
        }
        if (beanQualifiers.isEmpty()) {
            beanQualifiers.add(DEFAULT);
        }
        beanQualifiers.add(ANY);
        final Class<? extends Annotation> beanScope = scope;
        final String beanName = name;
        final boolean alternative = type.isAnnotationPresent(Alternative.class);
        return new Bean<T>() {
            @Override
            public Set<Type> getTypes() {
                return types;
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return beanQualifiers;
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return beanScope;
            }

            @Override
            public String getName() {
                return beanName;
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return stereotypes;
            }

            @Override
            public Class<?> getBeanClass() {
                return type.getJavaClass();
            }

            @Override
            public boolean isAlternative() {
                return alternative;
            }

            @Override
            public boolean isNullable() {
                return delegate.isNullable();
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return delegate.getInjectionPoints();
            }

            @Override
            public T create(final CreationalContext<T> creationalContext) {
                return cast(delegate.create(LoggingExtension.<CreationalContext<Object>>cast(creationalContext)));
            }

            @Override
            public void destroy(final T instance, final CreationalContext<T> creationalContext) {
                delegate.destroy(instance, LoggingExtension.<CreationalContext<Object>>cast(creationalContext));
            }

            @Override
            public String toString() {
                return delegate.toString();
            }
        };
    }
}
