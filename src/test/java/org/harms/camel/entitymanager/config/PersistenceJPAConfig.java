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
package org.harms.camel.entitymanager.config;

import org.apache.camel.component.jpa.JpaComponent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import java.util.Properties;


/**
 * Setup Entity Manager and Transaction manager
 */
@Configuration
@EnableTransactionManagement
public class PersistenceJPAConfig {

    @Bean(name = "emf")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setPackagesToScan("org.harms.camel.jpa.entitymanager");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        return em;
    }

    @Bean(name = "emf2")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory2() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setPackagesToScan("org.harms.camel.jpa.entitymanager");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        return em;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation(){
        return new PersistenceExceptionTranslationPostProcessor();
    }

    @Bean(name = "jpa")
    public JpaComponent jpaCompoment(@Qualifier("emf") EntityManagerFactory emf, PlatformTransactionManager tx){
        JpaComponent jpaComponent = new JpaComponent();
        jpaComponent.setEntityManagerFactory(emf);
        jpaComponent.setTransactionManager(tx);
        return jpaComponent;
    }

    @Bean(name = "jpa2")
    public JpaComponent jpaCompoment2(@Qualifier("emf2") EntityManagerFactory emf, PlatformTransactionManager tx){
        JpaComponent jpaComponent = new JpaComponent();
        jpaComponent.setEntityManagerFactory(emf);
        jpaComponent.setTransactionManager(tx);
        return jpaComponent;
    }


    private Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.archive.autodetection" ,"class");
        properties.setProperty("hibernate.dialect" ,"org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.connection.driver_class" ,"org.h2.Driver");
        properties.setProperty("hibernate.connection.url" ,"jdbc:h2:mem:test;DB_CLOSE_ON_EXIT=TRUE");
        properties.setProperty("hibernate.connection.user" ,"sa");
        properties.setProperty("hibernate.show_sql" ,"false");
        properties.setProperty("hibernate.id.new_generator_mappings", "true");
        properties.setProperty("hibernate.hbm2ddl.auto" ,"update");
        return properties;
    }
}