esmapper
========

esmapper is a simple, bidirectional mapper for reading and writing
Java objects from and to an Elasticsearch database. This mostly meant
for people who use ES as a schema-less database too, rather than just
as a search index.

For the time being, classes to be stored must extend a common "Entity"
base class that provides ID and version fields. This is scheduled to
be removed (and replaced with javax.persistence-style
annotations). Other than that, any POJO with JavaBeans-style
properties is supported.


Usage
-----

    import de.olafklischat.esmapper.Entity;

    public class City extends Entity {
        private String name;
        private int population;

        //default c'tor -- needed when reading instances back from the database
        public City(String name, int population) {
        }

        public City(String name, int population) {
            this.name = name;
            this.population = population;
        }

        //getters/setter omitted for brevity
    }

    ...


    import de.olafklischat.esmapper.Entity;

    public class Person extends Entity {
        private String name;
        private int age;
        private City homeTown;

        public Person() {
        }

        public Person(String name, int age, City homeTown) {
            this.name = name;
            this.age = age;
            this.homeTown = homeTown;
        }

        //getters/setter omitted for brevity
    }



    ...

    import org.elasticsearch.client.Client;
    import de.olafklischat.esmapper.EntityPersister;

    ...

    Client esClient = ....; //interface to the ES cluster
    EntityPersister ep = new EntityPersister(esClient, "es_index_name"); //facade to the object mapper

    City c = new City("Berlin", 3500000);
    ep.persist(c);
    Person p = new Person("Paul", 32, c);
    p.getId(); //=>null
    c.getId(); //=>null
    //persist p, cascading into any associated objects (c in this case)
    ep.persist(p, CascadeSpec.cascade());
    p.getId(); //=>[generated uuid]
    c.getId(); //=>[other generated uuid]

    //read back the Person with p's ID, again cascading into associated objects
    Person p2 = ep.findById(p.getId(), Person.class, CascadeSpec.cascade());
    p2.getName(); // "Paul"
    p2.getHomeTown().getName(); // "Berlin"


`CascadeSpec` is a recursive data structure that may be used to
specify more complex usage cascade/no-cascade policies based on
property names of the entity being loaded/stored as well entities
referenced by it, directly or indirectly. Simple collection-based
(1:N) associations are supported as well.

Read the EntityPersister
[unit test](https://github.com/multi-io/esmapper/blob/master/src/test/java/de/olafklischat/esmapper/EntityPersisterTest.java
"EntityPersisterTest") for a more thorough overview of what's possible.


JSON mapper
-----------

esmapper comes with its own internal JSON-JavaBean mapper (because
none of the existing ones were flexible enough when it came to hooking
into and customizing object (de)serializations).

There is a
[unit test](https://github.com/multi-io/esmapper/blob/master/src/test/java/de/olafklischat/esmapper/json/JsonConverterTest.java
"JsonConverterTest") for this as well.

This mapper can be used stand-alone; it does not know anything about
EntityPersister and related classes.

The mapper is currently based on Google Gson's streaming API (not
Gson's own object mapper) though. This dependency may be removed (or
made optional) in the future.
