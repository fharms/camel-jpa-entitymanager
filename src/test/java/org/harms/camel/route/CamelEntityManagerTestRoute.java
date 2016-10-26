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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
class CamelEntityManagerTestRoute extends RouteBuilder {

    public void configure() throws Exception {
        from(CamelEntityManagerTestRoutes.DIRECT_PERSIST_TEST.uri())
                .routeId(CamelEntityManagerTestRoutes.DIRECT_PERSIST_TEST.id())
                .transacted()
                .bean(CamelEntityManagerBean.class, "persistDog");

        from(CamelEntityManagerTestRoutes.DIRECT_FIND_TEST.uri())
                .routeId(CamelEntityManagerTestRoutes.DIRECT_FIND_TEST.id())
                .transacted()
                .bean(CamelEntityManagerBean.class, "findDog");

        from(CamelEntityManagerTestRoutes.DIRECT_COMPARE_HASHCODE_TEST.uri())
                .routeId(CamelEntityManagerTestRoutes.DIRECT_COMPARE_HASHCODE_TEST.id())
                .transacted()
                .bean(CamelEntityManagerBean.class, "compareHashCode");

        from(CamelEntityManagerTestRoutes.MANUEL_POLL_JPA_PRODUCER_TEST.uri())
                .routeId(CamelEntityManagerTestRoutes.MANUEL_POLL_JPA_PRODUCER_TEST.id())
                .enrich(CamelEntityManagerTestRoutes.DIRECT_JPA_PRODUCER_TEST.uri())
                .transacted()
                .bean(CamelEntityManagerBean.class, "findAnotherDog");

        from(CamelEntityManagerTestRoutes.MANUEL_POLL_JPA_CONSUMER_TEST.uri())
                .routeId(CamelEntityManagerTestRoutes.MANUEL_POLL_JPA_CONSUMER_TEST.id())
                .transacted()
                .pollEnrich(CamelEntityManagerTestRoutes.DIRECT_JPN_CONSUMER_TEST.uri(),0);

        from(CamelEntityManagerTestRoutes.DIRECT_NESTED_BEAN_TEST.uri())
                .routeId(CamelEntityManagerTestRoutes.DIRECT_NESTED_BEAN_TEST.id())
                .transacted()
                .bean(CamelEntityManagerBean.class, "persistWithNedstedCall");

        from(CamelEntityManagerTestRoutes.DIRECT_FIND_TEST_WITH_TWO_EM.uri())
                .routeId(CamelEntityManagerTestRoutes.DIRECT_FIND_TEST_WITH_TWO_EM.id())
                .transacted()
                .bean(CamelEntityManagerBean.class, "findAllDogs");

        from(CamelEntityManagerTestRoutes.DIRECT_ROLLBACK_TEST.uri())
                .routeId(CamelEntityManagerTestRoutes.DIRECT_ROLLBACK_TEST.id())
                .transacted()
                .bean(CamelEntityManagerBean.class, "forceRollback");

        from(CamelEntityManagerTestRoutes.DIRECT_NO_ANNOTATION_TEST.uri())
                .routeId(CamelEntityManagerTestRoutes.DIRECT_NO_ANNOTATION_TEST.id())
                .transacted()
                .bean(CamelEntityManagerBean.class, "persistWithNoAnnotation");

        from(CamelEntityManagerTestRoutes.DIRECT_WRONG_TYPE.uri())
                .routeId(CamelEntityManagerTestRoutes.DIRECT_WRONG_TYPE.id())
                .transacted()
                .bean(CamelEntityManagerBean.class, "persistWithNoAnnotation");

        from(CamelEntityManagerTestRoutes.DIRECT_NO_TX_ANNOTATION_TEST.uri())
                .routeId(CamelEntityManagerTestRoutes.DIRECT_NO_TX_ANNOTATION_TEST.id())
                .bean(BeanWithNoAnnotation.class, "noTxAnnotation");
    }

    /**
     * Created by fharms on 24/09/16.
     */
    @Configuration
    @ComponentScan(value = "org.harms.camel")
    public static class CamelContextConfiguration extends CamelConfiguration {

        @Autowired
        @Qualifier("jpa")
        JpaComponent jpaComponent1;

        @Autowired
        @Qualifier("jpa2")
        JpaComponent jpaComponent2;

        @Override
        protected void setupCamelContext(CamelContext camelContext) throws Exception {
            camelContext.addComponent("jpa", jpaComponent1);
            camelContext.addComponent("jpa2", jpaComponent2);
        }

    }
}