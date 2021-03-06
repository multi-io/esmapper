package de.olafklischat.esmapper;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import de.olafklischat.esmapper.annotations.LoadedFlag;
import de.olafklischat.esmapper.json.annotations.JsonIgnore;

@Entity
public class TestCity {
    private String name;
    private int population;
    private TestPerson mayor;
    private List<TestCity> sisterCities;
    
    public TestCity() {
    }

    public TestCity(String name, int population) {
        super();
        this.name = name;
        this.population = population;
    }

    private String id;

    private Long version;
    
    private boolean isLoaded = false;
    
    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Version
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
    
    @LoadedFlag
    public boolean isLoaded() {
        return isLoaded;
    }
    
    public void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
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
    
    public List<TestCity> getSisterCities() {
        return sisterCities;
    }
    
    public void setSisterCities(List<TestCity> sisterCities) {
        this.sisterCities = sisterCities;
    }
    
    @JsonIgnore
    public int getSisterCitiesCount() {
        return sisterCities == null ? 0 : sisterCities.size();
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
