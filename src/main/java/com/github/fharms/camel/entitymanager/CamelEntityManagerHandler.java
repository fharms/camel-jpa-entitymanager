/**
 * The MIT License
 * Copyright © 2016 Flemming Harms
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.fharms.camel.entitymanager;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.camel.Exchange;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scan for fields type {@link EntityManager} and annotated with {@link PersistenceContext}
 * and wrap the current entity manager proxy with a new proxy to control the flow. If
 * Camel has created a EntityManager we by pass the injected and use this in favour, unless
 * the field or method is annotated with {@link IgnoreCamelEntityManager}
 */
@Component
public class CamelEntityManagerHandler {

    public static final String CAMEL_ENTITY_MANAGER = "CamelEntityManager";

    private final ThreadLocal<EntityManager> entityManagerLocal = new ThreadLocal<>();

    public Object registerProxyHandler(Object bean) {
        List<Field> annotatedFields;

        try {
            annotatedFields = getAnnotatedFields(bean);
        } catch (IllegalAccessException e) {
            throw new BeanCreationException("Failed scanning for @PersistenceContext", e);
        }

        if (annotatedFields.size() == 0) {
            return bean;
        }

        annotatedFields.forEach(field -> {
            try {
                boolean currentAccessibleState = field.isAccessible();
                field.setAccessible(true);
                Object entityManagerProxy = createEntityManagerProxy(EntityManager.class,field.get(bean));
                field.set(bean, entityManagerProxy);
                field.setAccessible(currentAccessibleState);

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        return createBeanProxy(bean);
    }

    private <T> T createEntityManagerProxy(Class<T> interfaceClass, Object emProxy) {
        InvocationHandler handler = (proxy, method, args) -> {

            EntityManager em = entityManagerLocal.get() !=null ? entityManagerLocal.get() : (EntityManager) emProxy;
            switch (method.getName()) {
                case "hashCode":
                    return hashCode();
                case "equals":
                    return (em == args[0]);
                case "toString":
                    return "Camel EntityManager proxy ["+em.toString()+"]";
            }

            if (!em.isJoinedToTransaction()) {
                    em.joinTransaction();
            }
            return method.invoke(em, args);
        };
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, handler);
    }


    private Object createBeanProxy(Object bean) {
        MethodInterceptor handler = invocation -> {
            Method method = invocation.getMethod();
            switch (method.getName()) {
                case "hashCode":
                    return hashCode();
                case "equals":
                    return (invocation.getThis() == invocation.getArguments()[0]);
                case "toString":
                    return toString();
            }

            if (entityManagerLocal.get() == null && !method.isAnnotationPresent(IgnoreCamelEntityManager.class)) {
                Arrays.stream(invocation.getArguments())
                        .filter(f -> f instanceof Exchange)
                        .findFirst()
                        .map(Exchange.class::cast)
                        .map(o -> o.getIn().getHeader(CAMEL_ENTITY_MANAGER, EntityManager.class))
                        .ifPresent(em -> addThreadLocalEntityManager(em));
            }
            return invocation.proceed();
        };

        ProxyFactory factory = new ProxyFactory(bean);
        factory.addAdvice(handler);

        return factory.getProxy();
    }


    /**
     * Scan all fields for the {@link PersistenceContext} annotation and verify the type and return
     * a list of annotated fields. If the field is also annotated with  {@link IgnoreCamelEntityManager}
     * it will be ignored from the list
     *
     * @param bean Name of the bean to scan for annotation
     * @return List of fields annotated with {@link PersistenceContext}
     */
    private List<Field> getAnnotatedFields(Object bean) throws IllegalAccessException {
        List<Field> annotatedFields = new ArrayList<>();
        if (bean == null) {
            return annotatedFields;
        }

        Field[] declaredFields = bean.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            boolean currentAccessible = field.isAccessible();
            field.setAccessible(true);

            if (EntityManager.class.isAssignableFrom(field.getType()) &&
                    (field.isAnnotationPresent(PersistenceContext.class)) &&
                    (!field.isAnnotationPresent(IgnoreCamelEntityManager.class)) &&
                    (field.get(bean) != null)) {
                annotatedFields.add(field);
            }

            field.setAccessible(currentAccessible);

        }
        return annotatedFields;
    }

    private EntityManager addThreadLocalEntityManager(EntityManager em) {
        TransactionSynchronizationManager.registerSynchronization(
                new SessionCloseSynchronizationManager()
        );
        entityManagerLocal.set(em);
        return em;
    }

    /**
     * The {@link EntityManager}s is removed from the internal ThreadLocal when the transaction is complete regardless
     * if it commit or rollback. The cached entity manager is removed as it finally step.
     */
    private class SessionCloseSynchronizationManager extends TransactionSynchronizationAdapter {

        @Override
        public void afterCompletion(int status) {
            entityManagerLocal.remove();
        }
    }
}
