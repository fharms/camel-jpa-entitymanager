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
import org.apache.camel.Exchange;
import org.harms.camel.entity.Dog;
import org.harms.camel.entitymanager.CamelEntityManager;
import org.harms.camel.entitymanager.CamelEntityManagerHandler;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Test bean for testing injection of {@link EntityManager}
 */
@Transactional(value = "transactionManager")
public class CamelEntityManagerTestBean {

    @CamelEntityManager
    private EntityManager em;

    @CamelEntityManager(jpaComponent = "jpa2", ignoreCamelEntityManager = true)
    private EntityManager em2;

    public Dog persistDog(@Body Dog dogEntity){
        em.persist(dogEntity);
        return dogEntity;
    }

    public Dog findDog(@Body Long id){
        Dog dog = em.find(Dog.class, id);
        return dog;
    }

    public void findAnotherDog(Exchange exchange) {
        Object localEm = exchange.getIn().getHeader(CamelEntityManagerHandler.CAMEL_ENTITY_MANAGER, EntityManager.class);
        if (!em.equals(localEm)) {
            throw new RuntimeException("This is not good!");
        }
    }

    public void findAllDogs(Exchange exchange) {
        List<Dog> resultList = em.createQuery("select d from Dog d", Dog.class).getResultList();
        TypedQuery<Dog> dogQuery = em2.createQuery("select d from Dog d",Dog.class);
        exchange.getIn().setBody(dogQuery.getResultList());
    }

}
