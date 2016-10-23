/**
 * The MIT License
 * Copyright © 2016 Flemming Harms
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.harms.camel.route;

import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.spring.CamelSpringDelegatingTestContextLoader;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.CamelTestContextBootstrapper;
import org.harms.camel.entity.Dog;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.harms.camel.route.CamelEntityManagerRoutes.*;

@RunWith(CamelSpringRunner.class)
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextConfiguration(classes = CamelEntityManagerRoute.CamelContextConfiguration.class, loader = CamelSpringDelegatingTestContextLoader.class)
@Transactional
@Commit
public class CamelEntityManagerRouteTest {

    private Dog alphaDoc;

    @Produce
    private ProducerTemplate template;

    private TransactionTemplate txTemplate;

    @PersistenceContext(unitName = "emf")
    private EntityManager em;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Before
    public void setupDatabaseData() {
        txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        alphaDoc = createDog("Skippy", "Terrier");
        txTemplate.execute(new TransactionCallback<Exchange>() {
            public Exchange doInTransaction(TransactionStatus status) {
                em.persist(alphaDoc);
                return null;
            }
        });
    }

    @After
    public void cleanup() {
        em.createQuery("delete from Dog").executeUpdate();
    }

    @Test
    @DirtiesContext
    public void testEntityManagerInject() throws Exception {
        final Dog dog = createDog("Fiddo", "Beagle");

        txTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                return template.send(DIRECT_PERSIST.uri(), createExchange(dog));
            }
        });

        assertEquals(dog,findDog(dog.getId()));

        Exchange result = txTemplate.execute(new TransactionCallback<Exchange>() {
            public Exchange doInTransaction(TransactionStatus status) {
                return template.send(DIRECT_FIND.uri(), createExchange(new Long(dog.getId())));
            }
        });

        Dog dog2 = result.getIn().getBody(Dog.class);
        Assert.assertEquals(dog, dog2);
    }

    @Test
    @DirtiesContext
    public void testEntityManagerInjectFind() throws Exception {
        Exchange result = txTemplate.execute(new TransactionCallback<Exchange>() {
            public Exchange doInTransaction(TransactionStatus status) {
                return template.send(DIRECT_FIND.uri(), createExchange(new Long(alphaDoc.getId())));
            }
        });

        Dog dog2 = result.getIn().getBody(Dog.class);
        Assert.assertEquals(alphaDoc, dog2);
    }

    @Test
    @DirtiesContext
    public void testEntityManagerInjectWithJpaConsumer() throws Exception {
        final Dog boldDog = createDog("Bold", "Terrier");
        Exchange result = txTemplate.execute(new TransactionCallback<Exchange>() {
            public Exchange doInTransaction(TransactionStatus status) {
                return template.send(MANUEL_POLL_JPA.uri(), createExchange(boldDog));
            }
        });
        Dog dog = result.getIn().getBody(Dog.class);
        Assert.assertEquals(boldDog, dog);
    }

    @Test
    @DirtiesContext
    public void testEntityManager2Query() throws Exception {
        final Dog boldDog = createDog("Bold", "Terrier");
        txTemplate.execute(new TransactionCallback<Exchange>() {
            public Exchange doInTransaction(TransactionStatus status) {
                return template.send(DIRECT_PERSIST.uri(), createExchange(boldDog));
            }
        });

        assertNotNull(findDog(boldDog.getId()));
        Exchange result = txTemplate.execute(new TransactionCallback<Exchange>() {
            public Exchange doInTransaction(TransactionStatus status) {
                return template.send(DIRECT_JPA_MANAGER2.uri(), createExchange(null));
            }
        });
        List dogs = result.getIn().getBody(List.class);
        Assert.assertEquals(2, dogs.size());
    }

    private Dog findDog(Long id) {
        return em.find(Dog.class, id);
    }


    private Exchange createExchange(Object body) {
        DefaultExchange exchange = new DefaultExchange(template.getCamelContext());
        exchange.getIn().setBody(body);
        return exchange;
    }

    private Dog createDog(String petName, String race) {
        Dog dog = new Dog();
        dog.setPetName(petName);
        dog.setRace(race);
        return dog;
    }


}