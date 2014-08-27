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


[CascadeSpec](https://github.com/multi-io/esmapper/blob/master/src/main/java/de/olafklischat/esmapper/CascadeSpec.java)
is a recursive data structure that may be used to specify more complex
cascade/no-cascade policies based on property names of the entity
being loaded/stored as well entities referenced by it, directly or
indirectly. Simple collection-based (1:N) associations are supported
as well.

Read the EntityPersister
[unit test](https://github.com/multi-io/esmapper/blob/master/src/test/java/de/olafklischat/esmapper/EntityPersisterTest.java
"EntityPersisterTest") for a more thorough overview of what's possible.



JSON mapper
-----------

esmapper comes with its own internal bidirectional JSON-JavaBean
mapper, which it uses for converting the entities to JSON, which is
what ES expects, and back. I chose to roll my own mapper here because
all the existing ones that I looked at weren't flexible enough when it
came to hooking into and customizing object (de)serializations. Most
of them do have some customizability so that you can override what
gets written to (or read from) the JSON for each bean property that
the mapper encounters as it walks an input (output) object graph, but
none of those other mappers provided enough metadata to those
customization hooks so that you could always find out exactly which
node in the source or destination Java object graph the mapper is
currently handling. This is supported in esmapper's JSON mapper via
the
[PropertyPath](https://github.com/multi-io/esmapper/blob/master/src/main/java/de/olafklischat/esmapper/json/PropertyPath.java
"PropertyPath") data structure, and esmapper needs this to support
flexible user-defined cascade/no-cascade policies for entities to be
loaded or persisted.

There is a
[unit test](https://github.com/multi-io/esmapper/blob/master/src/test/java/de/olafklischat/esmapper/json/JsonConverterTest.java
"JsonConverterTest") for the JSON mapper as well.

This JSON mapper can be used stand-alone; it does not know anything
about EntityPersister and related classes. I'm planning to release it
as a separate project in the future.

The mapper is currently based on Google Gson's streaming API (not
Gson's own object mapper). This dependency may be removed (or made
optional) in the future.
