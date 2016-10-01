/**
 * The MIT License
 * Copyright (c) 2016 Flemming Harms
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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;

@Component
class CamelEntityManagerRoute extends RouteBuilder {

    static final String DIRECT_PERSIST = "direct:persist";
    static final String DIRECT_FIND = "direct:find";
    static final String DIRECT_TRASH = "direct:trash";
    static final String DIRECT_FIND_TRASH = "direct:find_trash";
    static final String DIRECT_PERSIST_ID = "persist";
    static final String DIRECT_FIND_ID = "find";

    public void configure() throws Exception {
        from(DIRECT_PERSIST)
                .routeId(DIRECT_PERSIST_ID)
                .transacted()
                .bean(CamelEntityManagerTestBean.class, "persistDog")
                .to(DIRECT_TRASH);

        from(DIRECT_FIND)
                .routeId(DIRECT_FIND_ID)
                .transacted()
                .bean(CamelEntityManagerTestBean.class, "findDog")
                .to(DIRECT_FIND_TRASH);
    }


    /**
     * Created by fharms on 24/09/16.
     */
    @Configuration
    @ComponentScan(value = "org.harms.camel")
    public static class CamelContextConfiguration extends CamelConfiguration {

        @Autowired
        EntityManagerFactory emf;

        @Autowired
        PlatformTransactionManager tx;

        @Override
        protected void setupCamelContext(CamelContext camelContext) throws Exception {
            JpaComponent jpaComponent = new JpaComponent();
            jpaComponent.setEntityManagerFactory(emf);
            jpaComponent.setTransactionManager(tx);
            camelContext.addComponent("jpa", jpaComponent);
        }

    }
}