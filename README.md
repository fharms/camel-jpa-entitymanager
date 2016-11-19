# Camel Entity Manager Bean Processor

Camel comes with a [JPA module](http://camel.apache.org/jpa.html) for using JPA within a route, 
by simply using the URI format "jpa:entityClassName[?options]". This make it real easy to make CRUD operation with Camel. 
But accessing Camels entity manager and join it with with current route transaction is a little more tricky, and require 
some manual work.

The Camel Entity Manager is a post processor that serves the purpose, to inject a proxy around the EntityManager 
and handle the logic around the injected entity manager. It support both use cases where the Camel has created 
the EntityManager as part of the JPA consumer or producer, or for none JPA consumers or producers.

# User guide

For using the Camel Entity Manager post processor add this dependency to your project
```xml
 <dependency>
      <artifactId>camel.jpa.entitymanager</artifactId>
      <groupId>com.github.fharms</groupId>
      <version>0.0.2</version>
 </dependency>
```

Annotate the field with @PersistenceContext and type EntityManager as you normal with do.

```java

  @javax.persistence.PersistenceContext(unitName = "emf")
  EntityManager em;
```

Working with multiple JPA components in camel
```java
  @javax.persistence.PersistenceContext(unitName = "emf")
  EntityManager em1;
  
  @javax.persistence.PersistenceContext(unitName = "emf2")
  EntityManager em2;
```

Force the Camel entity manager bean processor to ignore any Entity Manager created by the JPA consumer
```java
  
  @IgnoreCamelEntityManager
  @javax.persistence.PersistenceContext(unitName = "emf")
  EntityManager em;
  
  
  @IgnoreCamelEntityManager
  public void findMyEntity(Exchange exchange) {
     em.find(MyEntity.class, exchange.getIn().getBody(Integer.class))
  }
```
 
# Build the source
 
This is real easy, just execute the command : 

> mvn install 

Build Status
---------------

[![Build Status](https://travis-ci.org/fharms/camel-jpa-entitymanager.svg?branch=master)](https://travis-ci.org/fharms/camel-jpa-entitymanager)
