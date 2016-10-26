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

/**
 * All defined routes with URI and Id
 */
public enum CamelEntityManagerTestRoutes {

    DIRECT_PERSIST_TEST("direct:persistTest","directPersistTest"),
    MANUEL_POLL_JPA_PRODUCER_TEST("direct:manuelPollingTest","manuelPollingTest"),
    MANUEL_POLL_JPA_CONSUMER_TEST("direct:manuelPollingConsumerTest","manuelPollingConsumerTest"),
    DIRECT_JPN_CONSUMER_TEST("jpa:org.harms.camel.entity.Dog","directJpaPersistTest"),
    DIRECT_JPA_PRODUCER_TEST("jpa:org.harms.camel.entity.Dog","directJpaProducerTest"),
    DIRECT_FIND_TEST("direct:findTest","directFindTest"),
    DIRECT_FIND_TEST_WITH_TWO_EM("direct:findTestWithTwoEntityManagers", "findTestWithTwoEntityManagers"),
    DIRECT_COMPARE_HASHCODE_TEST("direct:compareHashCodeTest", "compareHashCodeTest"),
    DIRECT_NESTED_BEAN_TEST("direct:nestedBeanTest", "nestedBeanTest"),
    DIRECT_ROLLBACK_TEST("direct:rollbackTest", "rollbackTest"),
    DIRECT_WRONG_TYPE("direct:wrongType", "wrongType"),
    DIRECT_NO_ANNOTATION_TEST("direct:noAnnotationTest", "noAnnotationTest"),
    DIRECT_NO_TX_ANNOTATION_TEST("direct:noTxTest", "noTxTest");

    private final String routeUri;
    private final String routeId;

    CamelEntityManagerTestRoutes(String routeUri, String routeId) {
        this.routeUri = routeUri;
        this.routeId = routeId;
    }

    public String uri() {
        return routeUri;
    }

    public String id() {
        return routeId;
    }
}
