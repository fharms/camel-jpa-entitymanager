# Camel Entity Manager Bean Processor

Camel comes with a [JPA module](http://camel.apache.org/jpa.html) for using JPA within a route, 
by simply using the URI format "jpa:entityClassName[?options]". This make it real easy to make CRUD operation with Camel. 
Accessing Camels entity manager and join it with with current route transaction is a little more tricky, and require 
some manual work.

The CamelEntityManager is a post processor that serves the purpose, to inject a proxy around the EntityManager 
into a bean and join the current transaction started by Camel. It support both use cases where the Camel has created
the EntityManager as part of the JPA consumers or producers, or for none JPA consumers or producers.

# User guide

Annotate the field with @PersistenceContext and type EntityManager that should be injected in the bean.

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

Force the Camel entity manager bean processor to ignore
```java

  @org.harms.camel.entitymanager.IgnoreCamelEntityManager
  @javax.persistence.PersistenceContext(unitName = "emf")
  EntityManager em;
```
 
# Build the source
 
This is real easy, just execute the command : 

> mvn install 

Build Status
---------------

[![Build Status](https://travis-ci.org/fharms/camel-jpa-entitymanager.svg?branch=master)](https://travis-ci.org/fharms/camel-jpa-entitymanager)
