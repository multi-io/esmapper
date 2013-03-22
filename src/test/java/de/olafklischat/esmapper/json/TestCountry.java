package de.olafklischat.esmapper.json;

import java.util.LinkedList;
import java.util.List;

import de.olafklischat.esmapper.annotations.Ignore;
import de.olafklischat.esmapper.annotations.ImplClass;

public class TestCountry {

    private String name;
    private int population;
    private List<TestOrg> companies;
    private String ignored;

    public TestCountry() {
    }
    
    public TestCountry(String name, int population, List<TestOrg> companies, String ignored) {
        super();
        this.name = name;
        this.population = population;
        this.companies = companies;
        this.ignored = ignored;
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
    
    @ImplClass(LinkedList.class)
    public List<TestOrg> getCompanies() {
        return companies;
    }
    
    public void setCompanies(List<TestOrg> companies) {
        this.companies = companies;
    }

    @Ignore
    public String getIgnored() {
        return ignored;
    }
    
    public void setIgnored(String ignored) {
        this.ignored = ignored;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((companies == null) ? 0 : companies.hashCode());
        result = prime * result + ((ignored == null) ? 0 : ignored.hashCode());
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
        TestCountry other = (TestCountry) obj;
        if (companies == null) {
            if (other.companies != null)
                return false;
        } else if (!companies.equals(other.companies))
            return false;
        if (ignored == null) {
            if (other.ignored != null)
                return false;
        } else if (!ignored.equals(other.ignored))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (population != other.population)
            return false;
        return true;
    }

}
