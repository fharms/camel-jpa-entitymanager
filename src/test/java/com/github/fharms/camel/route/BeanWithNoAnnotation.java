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
import org.apache.camel.Body;
import org.apache.camel.Exchange;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Use for test purpose
 */
public class BeanWithNoAnnotation {

    @PersistenceContext(unitName = "emf")
    private EntityManager em;

    public void noTxAnnotation(@Body Dog dog){
        em.persist(dog);
    }

    public void noTxAnnotationWithExchange(Exchange exchange) {
        em.persist(exchange.getIn().getBody(Dog.class));
    }

    public void startTxFromRouteAndJoin(Exchange exchange) {
        EntityManager localEm = exchange.getIn().getHeader(CamelEntityManagerHandler.CAMEL_ENTITY_MANAGER, EntityManager.class);
        if (!em.equals(localEm)) {
            throw new RuntimeException("This is not good!, em.equals(localEm) should be equals");
        }
        Dog alphaDog = exchange.getIn().getBody(Dog.class);
        em.remove(em.merge(alphaDog));
        Dog dog = new Dog();
        dog.setPetName("Roxy");
        dog.setBreed("Afghan Hound");
        em.persist(dog);
    }


}
