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
package org.harms.camel.entitymanager;

import org.apache.camel.Exchange;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.model.ModelCamelContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Post processor for injecting fields with camel entity manager proxy when
 * annotated with the {@link CamelEntityManager}
 * <p>
 * The requirement for annotated field is it a type of {@link EntityManager} otherwise it
 * will throw an {@link IllegalStateException}.
 * </p>
 */
@Component
public class EntityManagerPostProcessor implements BeanPostProcessor {

    private ThreadLocal<HashMap<String, EntityManager>> entityManagerMapLocal = new ThreadLocal<HashMap<String, EntityManager>>() {
        @Override
        protected HashMap<String, EntityManager> initialValue() {
            return new HashMap<>();
        }
    };

    @Autowired
    private ApplicationContext context;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        List<Field> annotatedFields;

        try {
            annotatedFields = getAnnotatedFields(bean);
        } catch (IllegalAccessException e) {
            throw new BeanCreationException("Wrong bean");
        }

        if (annotatedFields.size() > 0) {

            for (Field field : annotatedFields) {
                try {
                    CamelEntityManager annotation = field.getAnnotation(CamelEntityManager.class);
                    EntityManager entityManagerProxy = getCamelEntityManagerProxy(EntityManager.class, annotation.jpaComponent());

                    boolean currentAccessibleState = field.isAccessible();

                    field.setAccessible(true);
                    field.set(bean, entityManagerProxy);

                    field.setAccessible(currentAccessibleState);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

            }

        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @SuppressWarnings("unchecked")
    private <T> T getCamelEntityManagerProxy(Class<T> interfaceClass, final String jpaComponentName) {

        InvocationHandler handler = new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                switch (method.getName()) {
                    case "equals":
                        return (proxy == args[0]);
                    case "getEntityManagerFactory":
                        return getEntityManagerFactory(jpaComponentName);
                    case "hashCode":
                        return hashCode();
                    case "toString":
                        return "Camel EntityManager proxy [" + getEntityManagerFactory(jpaComponentName) + "]";
                }

                EntityManager em = getEntityManager(jpaComponentName);
                if (em == null) {
                    em = createCamelEntityManager(jpaComponentName, args);

                    if (em == null) {
                        throw new RuntimeException("Unable to instantiate EntityManager");
                    }

                    HashMap<String, EntityManager> emMap = entityManagerMapLocal.get();
                    emMap.put(jpaComponentName, em);
                }

                em.joinTransaction();
                return method.invoke(em, args);
            }

        };

        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, handler);
    }

    /**
     * Return the {@link EntityManager} if it registered in the exchange message header, otherwise
     * it creates a new {@link EntityManager} from the registered {@link EntityManagerFactory}
     * @param jpaComponentName - The name of the registered jpa component containing {@link EntityManagerFactory}
     * @param args - Call parameters
     * @return a new {@link EntityManager}
     */
    private EntityManager createCamelEntityManager(String jpaComponentName, Object[] args) {

        TransactionSynchronizationManager.registerSynchronization(new SessionCloseSynchronizationManager(jpaComponentName));
        //This is not working as it now, because the parameter arguments is not the callers
        //but the parameters from the specific EntityManager method called.
        //TODO : Need to find a solution to how to use the CamelEntityManager header if possible
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Exchange) {
                    Exchange exchange = (Exchange) arg;
                    //Let's check if there is already an Entity manager register
                    EntityManager em = exchange.getIn().getHeader("CamelEntityManager", EntityManager.class);
                    //we return early because the entity manager is own by calling route
                    if (em != null) {
                        return em;
                    }
                }
            }
        }

        EntityManagerFactory emf = getEntityManagerFactory(jpaComponentName);
        if (emf != null) {
            return emf.createEntityManager();
        }
        return null;

    }

    /**
     * @param jpaComponentName - The name of the jpa component to lookup
     * @return return the {@link EntityManagerFactory} bind to the specified jpa component
     */
    private EntityManagerFactory getEntityManagerFactory(String jpaComponentName) {
        ModelCamelContext camelContext = context.getBean(ModelCamelContext.class);
        if (camelContext == null) {
            throw new IllegalStateException("No camel context registered");
        }

        JpaComponent jpaComponent = camelContext.getComponent(jpaComponentName, JpaComponent.class);

        if (jpaComponent == null) {
            throw new IllegalStateException(String.format("No camel jpa component was registered with the name %s", jpaComponentName));
        }

        return jpaComponent.getEntityManagerFactory();
    }

    /**
     * Scan all fields for the {@link CamelEntityManager} annotation and verify the type and return
     * a list of annotated fields
     * @param bean - Name of the bean to scan for annotation
     * @return - List of fields annotated with {@link CamelEntityManager}
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

            if (field.getType().isAssignableFrom(EntityManager.class) &&
                    (field.isAnnotationPresent(CamelEntityManager.class)) &&
                    (field.get(bean) == null)) {
                annotatedFields.add(field);
            }

            field.setAccessible(currentAccessible);

        }
        return annotatedFields;
    }

    private EntityManager getEntityManager(String jpaComponentName) {
        return entityManagerMapLocal.get().get(jpaComponentName);
    }

    /**
     * Close clear and close the {@link EntityManager} when the transaction is complete regardless
     * if it commit or rollback. The cached entity manager is removed as it finally step.
     */
    private class SessionCloseSynchronizationManager extends TransactionSynchronizationAdapter {

        private final String jpaComponentName;

        SessionCloseSynchronizationManager(String jpaComponentName) {
            this.jpaComponentName = jpaComponentName;
        }

        @Override
        public void afterCompletion(int status) {
            EntityManager em = getEntityManager(jpaComponentName);
            if (em != null && em.isOpen()) {
                em.clear();
                em.close();
            }
            entityManagerMapLocal.get().remove(jpaComponentName);
        }
    }

}
