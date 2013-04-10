package de.olafklischat.esmapper;

import de.olafklischat.esmapper.Entity;

public class TestPerson extends Entity {
    private String name;
    private int age;
    private String comment;
    private TestCity homeTown;
    private TestCity nativeTown;
    
    public static class AssociateRecord {
        private TestPerson associate;
        private String kind;
        public AssociateRecord() {
            // TODO Auto-generated constructor stub
        }
        public AssociateRecord(TestPerson associate, String kind) {
            super();
            this.associate = associate;
            this.kind = kind;
        }
        public TestPerson getAssociate() {
            return associate;
        }
        public void setAssociate(TestPerson associate) {
            this.associate = associate;
        }
        public String getKind() {
            return kind;
        }
        public void setKind(String kind) {
            this.kind = kind;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((associate == null) ? 0 : associate.hashCode());
            result = prime * result + ((kind == null) ? 0 : kind.hashCode());
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
            AssociateRecord other = (AssociateRecord) obj;
            if (associate == null) {
                if (other.associate != null)
                    return false;
            } else if (!associate.equals(other.associate))
                return false;
            if (kind == null) {
                if (other.kind != null)
                    return false;
            } else if (!kind.equals(other.kind))
                return false;
            return true;
        }
    }
    
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
    public TestCity getNativeTown() {
        return nativeTown;
    }
    public void setNativeTown(TestCity nativeTown) {
        this.nativeTown = nativeTown;
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

    @Override
    public String toString() {
        return getName();
    }

}
