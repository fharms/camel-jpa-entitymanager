# Camel Entity Manager (DRAFT)

Camel comes with a [JPA module](http://camel.apache.org/jpa.html) for using JPA within a route, 
by simply using the URI format "jpa:entityClassName[?options]". This make it real easy to make CRUD operation with Camel. 
But accessing the entity manager and join it with  transaction started by camel it little more tricky, and require 
some manual work.

The CamelEntityManager is a annotation that serves the purpose, to inject a EntityManager into a bean
and join the current transaction started by Camel. It support both use cases where the Camel has created
the EntityManager as part of the JPA consumer or producer, or for none JPA consumer or producer.

# User guide

Annotate the field with @CamelEntityManager and type EntityManager that should be injected in the bean.

```java

  @org.harms.camel.entitymanager.CamelEntityManager
  EntityManager em;
```

Working with multiple JPA components in camel
```java

  @org.harms.camel.entitymanager.CamelEntityManager(jpaComponent="jpa1")
  EntityManager em;
```

Ignoring the Camel entity manager and inject a new entity manager
```java

  @org.harms.camel.entitymanager.CamelEntityManager(ignoreCamelEntityManager=true)
  EntityManager em;
```
 
# Build the source
 
This is really easy, just execute the command : 

> mvn install 

Build Status
---------------

[![Build Status](https://travis-ci.org/fharms/camel-jpa-entitymanager.svg?branch=master)](https://travis-ci.org/fharms/camel-jpa-entitymanager)
