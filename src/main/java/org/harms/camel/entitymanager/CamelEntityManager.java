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
package org.harms.camel.entitymanager;

import javax.interceptor.InterceptorBinding;
import javax.persistence.EntityManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * Inject the {@link EntityManager} into the annotated field, the field type should
 * be {@link EntityManager} otherwise it will throw an {@link IllegalStateException}.
 * <p>
 * Multiple fields can be annotated with the different {@link org.apache.camel.component.jpa.JpaComponent}
 * specified.
 * </p>
 * <p>
 * If Camel has created a {@link EntityManager} this will be injected in to the field instead.
 * This can be overwritten by adding the field ignoreCamelEntityManager=true and new entity manager
 * will be created instead
 * </p>
 * <pre>
 * {@code
 *
 *@literal @CamelEntityManager(jpaComponent="jpa1")
 * EntityManager em1;
 *
 *@literal @CamelEntityManager(jpaComponent="jpa2", ignoreCamelEntityManager=true)
 * EntityManager em2;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD})
@InterceptorBinding
public @interface CamelEntityManager {

    String jpaComponent() default "jpa";

    boolean ignoreCamelEntityManager() default false;
}