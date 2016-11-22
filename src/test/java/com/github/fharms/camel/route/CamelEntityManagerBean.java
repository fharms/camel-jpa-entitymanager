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
package com.github.fharms.camel.route;

import com.github.fharms.camel.entity.Dog;
import com.github.fharms.camel.entitymanager.CamelEntityManagerHandler;
import com.github.fharms.camel.entitymanager.IgnoreCamelEntityManager;
import org.apache.camel.Body;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.jpa.JpaComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import static org.junit.Assert.assertEquals;

/**
 * Test bean for testing injection of {@link EntityManager}
 */
@Component
@Transactional(value = "transactionManager")
public class CamelEntityManagerBean {

    @PersistenceContext(unitName = "emf")
    private EntityManager em;

    @PersistenceContext(unitName = "emf2")
    private EntityManager em2;

    private EntityManager em3;

    @PersistenceContext(unitName = "emf")
    private Object em4;

    @PersistenceContext(unitName = "emf")
    private EntityManager em5;

    @PersistenceContext(unitName = "emf")
    EntityManager em6;

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
        EntityManager localEm = exchange.getIn().getHeader(CamelEntityManagerHandler.CAMEL_ENTITY_MANAGER, EntityManager.class);
        if (!em.equals(localEm)) {
           throw new RuntimeException("This is not good!, em.equals(localEm) is not equals");
        }
        Dog dog = new Dog();
        dog.setPetName("Buddy");
        dog.setBreed("Norwegian Lundehund");
        em.persist(dog);
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

    public void forceRollback(Exchange exchange) {
        Dog dog = exchange.getIn().getBody(Dog.class);
        assertEquals("Skippy",dog.getPetName());
        assertEquals("Terrier",dog.getRace());
        em.persist(new Object());
    }

    public void forceRollbackFromRoute(Exchange exchange) {
        Dog dog = new Dog();
        dog.setPetName("Buddy");
        dog.setBreed("Norwegian Lundehund");
        em.persist(dog);
    }

    public void persistWithNoAnnotation(@Body Dog dog){
        em3.persist(dog);
    }

    public void persistWithPersistenceContext(@Body Dog dog){
        em6.joinTransaction();
        em6.persist(dog);
    }

    @IgnoreCamelEntityManager
    public void ignoreCamelEntityManager(Exchange exchange) {
        EntityManager localEm = exchange.getIn().getHeader(CamelEntityManagerHandler.CAMEL_ENTITY_MANAGER, EntityManager.class);
        if (em.equals(localEm)) {
            throw new RuntimeException("This is not good!, em.equals(localEm) should not be equals");
        }
    }
}
