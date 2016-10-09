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

import org.apache.camel.BeanInject;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.CamelSpringDelegatingTestContextLoader;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.CamelTestContextBootstrapper;
import org.apache.camel.test.spring.UseAdviceWith;
import org.harms.camel.entity.Dog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;

import javax.persistence.EntityManager;

/**
 * Created by fharms on 24/09/16.
 */
@RunWith(CamelSpringJUnit4ClassRunner.class)
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextConfiguration(classes = CamelEntityManagerRoute.CamelContextConfiguration.class, loader = CamelSpringDelegatingTestContextLoader.class)
@UseAdviceWith
public class CamelEntityManagerRouteTest {

    @EndpointInject(uri = "direct:persist")
    private ProducerTemplate producerPersist;

    @EndpointInject(uri = "direct:find")
    private ProducerTemplate producerFind;

    @EndpointInject(uri = "direct:manuelPolling")
    private ProducerTemplate manuelPolling;

    @EndpointInject(uri = "mock:persistResult")
    private MockEndpoint resultPersistEndpoint;

    @EndpointInject(uri = "mock:findResult")
    private MockEndpoint resultFindEndpoint;

    @BeanInject
    private JpaComponent jpaComponent;

    private Dog alphaDoc;

    @Before
    public void setupDatabaseData() {
        EntityManager em = jpaComponent.getEntityManagerFactory().createEntityManager();
        em.getTransaction().begin();
        alphaDoc = createDog("Skippy", "Terrier");
        em.persist(alphaDoc);
        em.flush();
        em.getTransaction().commit();
    }

    @Before
    public void adviceWith() throws Exception {
        final ModelCamelContext context = (ModelCamelContext) producerPersist.getCamelContext();
        context.getRouteDefinition(CamelEntityManagerRoutes.DIRECT_PERSIST.id()).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(CamelEntityManagerRoutes.END_OF_LINE1.uri())
                        .to(resultPersistEndpoint.getEndpointUri());

            }
        });

        context.getRouteDefinition(CamelEntityManagerRoutes.DIRECT_FIND.id()).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(CamelEntityManagerRoutes.END_OF_LINE2.uri())
                    .to(resultFindEndpoint.getEndpointUri());

            }
        });

        context.getRouteDefinition(CamelEntityManagerRoutes.MANUEL_POLL_JPA.id()).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(CamelEntityManagerRoutes.END_OF_LINE3.uri())
                        .to(resultFindEndpoint.getEndpointUri());

            }
        });
        context.start();
    }

    @Test
    public void testEntityManagerInject() throws Exception {
        resultPersistEndpoint.reset();
        resultPersistEndpoint.setExpectedCount(1);

        Dog dog = createDog("Fiddo","Beagle");

        producerPersist.sendBody(dog);

        resultPersistEndpoint.assertIsSatisfied();
        Exchange exchange = resultPersistEndpoint.getReceivedExchanges().get(0);
        Dog dog2 = exchange.getIn().getBody(Dog.class);
        Assert.assertEquals(dog, dog2);

        resultFindEndpoint.reset();
        resultFindEndpoint.setExpectedCount(1);
        producerFind.sendBody(new Long(dog2.getId()));
        resultFindEndpoint.assertIsSatisfied();

        exchange = resultFindEndpoint.getReceivedExchanges().get(0);
        dog2 = exchange.getIn().getBody(Dog.class);
        Assert.assertEquals(dog, dog2);

    }

    @Test
    public void testEntityManagerInjectWithJpaConsumer() throws Exception {
        Dog boldDog = createDog("Bold", "Terrier");
        resultFindEndpoint.setExpectedCount(1);
        manuelPolling.sendBody(boldDog);
        resultFindEndpoint.assertIsSatisfied();
        Exchange exchange = resultFindEndpoint.getReceivedExchanges().get(0);
        Dog dog = exchange.getIn().getBody(Dog.class);
        Assert.assertEquals(boldDog,dog);

    }

    private Dog createDog(String petName, String race) {
        Dog dog = new Dog();
        dog.setPetName(petName);
        dog.setRace(race);
        return dog;
    }

}