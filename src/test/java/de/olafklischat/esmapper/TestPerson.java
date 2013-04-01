package de.olafklischat.esmapper;

import de.olafklischat.esmapper.Entity;

public class TestPerson extends Entity {
    private String name;
    private int age;
    private String comment;
    private TestCity homeTown;
    
    public TestPerson() {
    }
    
    public TestPerson(String name, int age, String comment) {
        super();
        this.name = name;
        this.age = age;
        this.comment = comment;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public TestCity getHomeTown() {
        return homeTown;
    }
    public void setHomeTown(TestCity homeTown) {
        this.homeTown = homeTown;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + age;
        result = prime * result + ((comment == null) ? 0 : comment.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TestPerson other = (TestPerson) obj;
        if (age != other.age)
            return false;
        if (comment == null) {
            if (other.comment != null)
                return false;
        } else if (!comment.equals(other.comment))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
