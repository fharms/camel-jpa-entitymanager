package org.harms.camel.route;

import org.harms.camel.entity.Dog;

import javax.persistence.EntityManager;

/**
 * Created by fharms on 10/10/16.
 */
public class BeanWithNoAnnotation {

    private EntityManager em;

    public void findDog(){
        em.find(Dog.class,new Long(1));
    }
}
