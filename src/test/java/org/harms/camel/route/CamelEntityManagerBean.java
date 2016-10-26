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
package org.harms.camel.route;

import org.apache.camel.Body;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.jpa.JpaComponent;
import org.harms.camel.entity.Dog;
import org.harms.camel.entitymanager.CamelEntityManager;
import org.harms.camel.entitymanager.CamelEntityManagerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

/**
 * Test bean for testing injection of {@link EntityManager}
 */
@Component
@Transactional(value = "transactionManager")
public class CamelEntityManagerBean {

    @CamelEntityManager
    private EntityManager em;

    @CamelEntityManager(jpaComponent = "jpa2", ignoreCamelEntityManager = true)
    private EntityManager em2;

    private EntityManager em3;

    @CamelEntityManager
    private Object em4;

    @CamelEntityManager
    private EntityManager em5;

    @Autowired
    CamelEntityManagerNestedBean nBean;

    public Dog persistDog(@Body Dog dogEntity) {
        em.persist(dogEntity);
        return dogEntity;
    }

    public Dog findDog(@Body Long id) {
        return em.find(Dog.class, id);
    }

    public void findAnotherDog(Exchange exchange) {
        EntityManager localEm = exchange.getProperty(CamelEntityManagerHandler.CAMEL_ENTITY_MANAGER, EntityManager.class);
        if (!em.equals(localEm)) {
            throw new RuntimeException("This is not good!");
        }
    }

    public void findAllDogs(Exchange exchange) {
        TypedQuery<Dog> dogQuery = em2.createQuery("select d from Dog d", Dog.class);
        exchange.getIn().setBody(dogQuery.getResultList());
    }

    public Integer compareHashCode(Exchange exchange) {
        CamelContext context = exchange.getContext();
        EntityManagerFactory currentEm = em.getEntityManagerFactory();
        if (currentEm.hashCode() != context.getComponent("jpa", JpaComponent.class).getEntityManagerFactory().hashCode()) {
            throw new RuntimeException("This is not good!");
        }
        return currentEm.hashCode();
    }

    public Dog persistWithNedstedCall(@Body Dog body) {
        em.persist(body);
        return nBean.persistDog(em);
    }

    public void forceRollback() {
        em.persist(new Object());
    }

    public void persistWithNoAnnotation(@Body Dog dog){
        em3.persist(dog);
    }
}
