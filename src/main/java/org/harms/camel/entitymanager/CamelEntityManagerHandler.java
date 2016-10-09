package org.harms.camel.entitymanager;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.camel.Exchange;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.model.ModelCamelContext;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Create a {@link EntityManager} proxy for the EntityManager for handling logic regarding creating and terminate
 * new entity managers on invoking specific methods. For reusing the EntityManager inside a transaction
 * scope it's cashed as part of the {@link ThreadLocal} with the {@link JpaComponent} as key.
 *
 * <p>
 * When the transaction is completed it will clear and remove the EntityManager for manual created
 * entity managers. For Camel specific entity managers is removed from the ThreadLocal cache and Camel
 * is responsible for clean and terminate the EntityManager.
 * </p>
 *
 * <p>
 * If an EntityManager was created by Camel it will use the EntityManager registered in the header "CamelEntityManager".
 * The bean proxy created as part of the {@link EntityManager} proxy intercept all method call with the parameter
 * {@link Exchange} and retrieve the entity manager from the header.</br>
 * It possible to ignore the Camel Entity manager and let the CamelEntityManagerHandler created a new by added
 * the {@code @CamelEntityManager(ignoreCamelEntityManager = true)}
 * </p>
 *
 */
@Component
public class CamelEntityManagerHandler {

    public static final String CAMEL_ENTITY_MANAGER = "CamelEntityManager";

    private ThreadLocal<HashMap<String, EntityManager>> entityManagerMapLocal = new ThreadLocal<HashMap<String, EntityManager>>() {
        @Override
        protected HashMap<String, EntityManager> initialValue() {
            return new HashMap<>();
        }
    };

    @Autowired
    private ApplicationContext context;

    public Object registerProxyHandler(Object bean) {
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
                    EntityManager entityManagerProxy = createCamelEntityManagerProxy(EntityManager.class, annotation);

                    boolean currentAccessibleState = field.isAccessible();

                    field.setAccessible(true);
                    field.set(bean, entityManagerProxy);
                    field.setAccessible(currentAccessibleState);

                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return createBeanProxy(bean);

        }
        return bean;

    }

    private Object createBeanProxy(Object bean) {

        MethodInterceptor handler = new MethodInterceptor() {

            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                Method method = invocation.getMethod();
                switch (method.getName()) {
                    case "equals":
                        return (invocation.getThis() == invocation.getArguments()[0]);
                    case "hashCode":
                        return hashCode();
                    case "toString":
                        return toString();
                }

                for (Object arg : invocation.getArguments()) {
                    if (arg instanceof Exchange) {
                        //Let's check if there is already an Entity manager register
                        EntityManager em = ((Exchange) arg).getIn().getHeader(CAMEL_ENTITY_MANAGER, EntityManager.class);
                        //set the thread local with the entity manager for later use
                        if (em != null) {
                            addThreadLocalEntityManager(em, CAMEL_ENTITY_MANAGER, false);
                        }
                        break;
                    }
                }
                return invocation.proceed();
            }

        };

        ProxyFactory factory = new ProxyFactory(bean);
        factory.addAdvice(handler);

        return factory.getProxy();

    }

    @SuppressWarnings("unchecked")
    private <T> T createCamelEntityManagerProxy(Class<T> interfaceClass, final CamelEntityManager jpaComponent) {

        InvocationHandler handler = new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                EntityManager em = getEntityManager(jpaComponent.jpaComponent(), jpaComponent.ignoreCamelEntityManager());
                switch (method.getName()) {
                    case "equals":
                        return (em == args[0]);
                    case "getEntityManagerFactory":
                        return getEntityManagerFactory(jpaComponent.jpaComponent());
                    case "hashCode":
                        return hashCode();
                    case "toString":
                        return "Camel EntityManager proxy [" + getEntityManagerFactory(jpaComponent.jpaComponent()) + "]";
                }

                if (em == null) {
                    em = createCamelEntityManager(jpaComponent.jpaComponent());
                    if (em == null) {
                        throw new RuntimeException("Unable to instantiate EntityManager");
                    }
                    addThreadLocalEntityManager(em, jpaComponent.jpaComponent(), jpaComponent.ignoreCamelEntityManager());
                }

                em.joinTransaction();
                return method.invoke(em, args);
            }

        };

        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, handler);
    }

    /**
     * Scan all fields for the {@link CamelEntityManager} annotation and verify the type and return
     * a list of annotated fields
     *
     * @param bean Name of the bean to scan for annotation
     * @return List of fields annotated with {@link CamelEntityManager}
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

    private void addThreadLocalEntityManager(EntityManager em, String jpaComponentName, boolean ignoreCamelEntityManager) {
        TransactionSynchronizationManager.registerSynchronization(
                new SessionCloseSynchronizationManager(jpaComponentName, ignoreCamelEntityManager)
        );
        HashMap<String, EntityManager> emMap = entityManagerMapLocal.get();
        emMap.put(jpaComponentName, em);
    }

    /**
     * Return the {@link EntityManager} if it registered in the exchange message header, otherwise
     * it creates a new {@link EntityManager} from the registered {@link EntityManagerFactory}
     *
     * @param jpaComponentName The name of the registered jpa component containing {@link EntityManagerFactory}
     * @return a new {@link EntityManager}
     */
    private EntityManager createCamelEntityManager(String jpaComponentName) {

        EntityManagerFactory emf = getEntityManagerFactory(jpaComponentName);
        if (emf != null) {
            return emf.createEntityManager();
        }
        return null;

    }

    /**
     * @param jpaComponentName The name of the jpa component to lookup
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
     * Return the registered thread local {@link EntityManager}. If the Camel entity manager
     * is registered it will have precedence, else the {@link EntityManager} for the specified
     * JPA component is returned.
     *
     * @param jpaComponentName         Name of the JPA component
     * @param ignoreCamelEntityManager True if should ignore the Camel Entity Manager
     */
    private EntityManager getEntityManager(String jpaComponentName, boolean ignoreCamelEntityManager) {
        if (!ignoreCamelEntityManager) {
            EntityManager em = entityManagerMapLocal.get().get(CAMEL_ENTITY_MANAGER);
            if (em != null) {
                return em;
            }
        }
        return entityManagerMapLocal.get().get(jpaComponentName);
    }


    /**
     * Clear and close for manual created {@link EntityManager}s when the transaction is complete regardless
     * if it commit or rollback. The cached entity manager is removed as it finally step.
     * <p>
     * For CamelEntityManager it's removed from the internal ThreadLocal since Camel will
     * do the rest of the job.
     * </p>
     */
    private class SessionCloseSynchronizationManager extends TransactionSynchronizationAdapter {

        private final String jpaComponentName;
        private final boolean ignoreCamelEntityManager;

        SessionCloseSynchronizationManager(String jpaComponentName, boolean ignoreCamelEntityManager) {
            this.jpaComponentName = jpaComponentName;
            this.ignoreCamelEntityManager = ignoreCamelEntityManager;
        }

        @Override
        public void afterCompletion(int status) {
            EntityManager em = getEntityManager(jpaComponentName, ignoreCamelEntityManager);
            if (em != null && em.isOpen() && !CAMEL_ENTITY_MANAGER.equals(jpaComponentName)) {
                em.clear();
                em.close();
            }
            entityManagerMapLocal.get().remove(jpaComponentName);
        }
    }
}
