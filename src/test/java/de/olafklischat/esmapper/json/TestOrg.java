package de.olafklischat.esmapper.json;

public class TestOrg {

    private String name;
    private int revenue;
    private int nrOfEmployees;
    
    public TestOrg() {
    }
    
    public TestOrg(String name, int revenue, int nrOfEmployees) {
        super();
        this.name = name;
        this.revenue = revenue;
        this.nrOfEmployees = nrOfEmployees;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRevenue() {
        return revenue;
    }

    public void setRevenue(int revenue) {
        this.revenue = revenue;
    }

    public int getNrOfEmployees() {
        return nrOfEmployees;
    }

    public void setNrOfEmployees(int nrOfEmployees) {
        this.nrOfEmployees = nrOfEmployees;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + nrOfEmployees;
        result = prime * result + revenue;
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
        TestOrg other = (TestOrg) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nrOfEmployees != other.nrOfEmployees)
            return false;
        if (revenue != other.revenue)
            return false;
        return true;
    }

}
