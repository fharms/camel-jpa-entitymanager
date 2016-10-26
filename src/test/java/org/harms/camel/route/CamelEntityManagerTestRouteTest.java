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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.harms.camel.route.CamelEntityManagerTestRoutes.*;

@RunWith(CamelSpringRunner.class)
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextConfiguration(classes = CamelEntityManagerTestRoute.CamelContextConfiguration.class, loader = CamelSpringDelegatingTestContextLoader.class)
@Transactional
@Commit
public class CamelEntityManagerTestRouteTest {

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
        txTemplate.execute((TransactionCallback) status -> {
            em.persist(alphaDoc);
            return null;
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

        txTemplate.execute(status -> template.send(DIRECT_PERSIST_TEST.uri(), createExchange(dog)));

        assertEquals(dog, findDog(dog.getId()));

        Exchange result = txTemplate.execute(status -> template.send(DIRECT_FIND_TEST.uri(), createExchange(dog.getId())));

        Dog dog2 = result.getIn().getBody(Dog.class);
        Assert.assertEquals(dog, dog2);
    }

    @Test
    @DirtiesContext
    public void testEntityManagerInjectFind() throws Exception {
        Exchange result = txTemplate.execute(status -> template.send(DIRECT_FIND_TEST.uri(), createExchange(alphaDoc.getId())));

        Dog dog2 = result.getIn().getBody(Dog.class);
        Assert.assertEquals(alphaDoc, dog2);
    }

    @Test
    @DirtiesContext
    public void testEntityManagerInjectWithJpaProducer() throws Exception {
        final Dog boldDog = createDog("Bold", "Terrier");
        Exchange result = txTemplate.execute(status -> template.send(MANUEL_POLL_JPA_PRODUCER_TEST.uri(), createExchange(boldDog)));
        Long id = result.getIn().getBody(Dog.class).getId();
        Assert.assertEquals(boldDog, findDog(id));
    }

    @Test
    @DirtiesContext
    public void testEntityManagerInjectWithJpaConsumer() throws Exception {
        Exchange result = txTemplate.execute(status -> template.send(MANUEL_POLL_JPA_CONSUMER_TEST.uri(), createExchange("")));
        Dog dog = result.getIn().getBody(Dog.class);
        Assert.assertEquals(alphaDoc, dog);
    }


    @Test
    @DirtiesContext
    public void testEntityManagerInjectCompareHashCode() throws Exception {
        Exchange result = txTemplate.execute(status -> template.send(DIRECT_COMPARE_HASHCODE_TEST.uri(), createExchange(null)));
        Integer hashcode = result.getIn().getBody(Integer.class);
        assertNotNull(hashcode);
    }

    @Test
    @DirtiesContext
    public void testEntityManagerNestedCalls() throws Exception {
        Exchange result = txTemplate.execute(status -> template.send(DIRECT_NESTED_BEAN_TEST.uri(), createExchange(createDog("Bold", "Terrier"))));
        Dog dog = result.getIn().getBody(Dog.class);
        assertEquals("Joe", dog.getPetName());
        assertEquals("German Shepherd", dog.getRace());
    }

    @Test
    @DirtiesContext
    public void testWithTwoEntityManagersQuery() throws Exception {
        final Dog boldDog = createDog("Bold", "Terrier");
        txTemplate.execute(status -> template.send(DIRECT_PERSIST_TEST.uri(), createExchange(boldDog)));

        assertNotNull(findDog(boldDog.getId()));
        Exchange result = txTemplate.execute(status -> template.send(DIRECT_FIND_TEST_WITH_TWO_EM.uri(), createExchange(null)));
        List dogs = result.getIn().getBody(List.class);
        Assert.assertEquals(2, dogs.size());
    }
    @Rule
    public ExpectedException RollbackThrown = ExpectedException.none();

    @Test
    @DirtiesContext
    public void testNoAnnotation() throws Exception {
        RollbackThrown.expect(TransactionSystemException.class);
        final Dog boldDog = createDog("Bold", "Terrier");
        txTemplate.execute(status -> template.send(DIRECT_NO_ANNOTATION_TEST.uri(), createExchange(boldDog)));
    }

    @Test
    @DirtiesContext
    public void testNoTransactionAnnotation() throws Exception {
        final Dog boldDog = createDog("Bold", "Terrier");
        txTemplate.execute(status -> template.send(DIRECT_NO_TX_ANNOTATION_TEST.uri(), createExchange(boldDog)));
        assertNotNull(findDog(boldDog.getId()));
    }

    @Test
    @DirtiesContext
    public void testRollback() throws Exception {
        RollbackThrown.expect(TransactionSystemException.class);
        RollbackThrown.expectMessage("Could not commit JPA transaction");
        txTemplate.execute(status -> template.send(DIRECT_ROLLBACK_TEST.uri(), createExchange(null)));
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