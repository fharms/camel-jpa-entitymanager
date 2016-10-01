/**
 * The MIT License
 * Copyright (c) 2016 Flemming Harms
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

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.CamelSpringDelegatingTestContextLoader;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.CamelTestContextBootstrapper;
import org.harms.camel.entity.Dog;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created by fharms on 24/09/16.
 */
@RunWith(CamelSpringJUnit4ClassRunner.class)
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextConfiguration(classes = CamelEntityManagerRoute.CamelContextConfiguration.class, loader = CamelSpringDelegatingTestContextLoader.class)
public class CamelEntityManagerRouteTest {

    @EndpointInject(uri = CamelEntityManagerRoute.DIRECT_PERSIST)
    ProducerTemplate producerPersist;

    @EndpointInject(uri = CamelEntityManagerRoute.DIRECT_FIND)
    ProducerTemplate producerFind;


    @EndpointInject(uri = "mock:persistResult")
    protected MockEndpoint resultPersistEndpoint;

    @EndpointInject(uri = "mock:findResult")
    protected MockEndpoint resultFindEndpoint;

    @Test
    public void testEntityManagerInject() throws Exception {
        ModelCamelContext context = (ModelCamelContext) producerPersist.getCamelContext();
        resultPersistEndpoint.setExpectedCount(1);
        resultFindEndpoint.setExpectedCount(1);
        context.getRouteDefinition(CamelEntityManagerRoute.DIRECT_PERSIST_ID).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(CamelEntityManagerRoute.DIRECT_TRASH)
                        .skipSendToOriginalEndpoint()
                        .to(resultPersistEndpoint.getEndpointUri());

            }
        });

        context.getRouteDefinition(CamelEntityManagerRoute.DIRECT_FIND_ID).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(CamelEntityManagerRoute.DIRECT_FIND_TRASH)
                        .skipSendToOriginalEndpoint()
                        .to(resultFindEndpoint.getEndpointUri());

            }
        });

        Dog dog = new Dog();
        dog.setPetName("Fiddo");
        dog.setRace("Beagle");

        producerPersist.sendBody(dog);
        resultPersistEndpoint.assertIsSatisfied();
        Exchange exchange = resultPersistEndpoint.getReceivedExchanges().get(0);
        Dog dog2 = exchange.getIn().getBody(Dog.class);
        Assert.assertEquals(dog,dog2);

        producerFind.sendBody(new Long(dog2.getId()));
        resultFindEndpoint.assertIsSatisfied();
        exchange = resultFindEndpoint.getReceivedExchanges().get(0);
        dog2 = exchange.getIn().getBody(Dog.class);
        Assert.assertEquals(dog,dog2);

    }

}