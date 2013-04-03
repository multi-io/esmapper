package de.olafklischat.esmapper;

import java.util.Collection;

import de.olafklischat.esmapper.Entity;

public class TestCity extends Entity {
    private String name;
    private int population;
    private TestPerson mayor;
    private Collection<TestCity> sisterCities;
    
    public TestCity() {
    }

    public TestCity(String name, int population) {
        super();
        this.name = name;
        this.population = population;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }
    
    public TestPerson getMayor() {
        return mayor;
    }
    
    public void setMayor(TestPerson mayor) {
        this.mayor = mayor;
    }
    
    public Collection<TestCity> getSisterCities() {
        return sisterCities;
    }
    
    public void setSisterCities(Collection<TestCity> sisterCities) {
        this.sisterCities = sisterCities;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + population;
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
        TestCity other = (TestCity) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (population != other.population)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }

}
