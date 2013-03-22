package de.olafklischat.esmapper.json;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

public class TestProduct {
    private String name;
    private int price;
    private List<String> ingredients;
    private TestOrg producer;
    
    public TestProduct() {
    }
    
    public TestProduct(String name, int price, List<String> ingredients, TestOrg producer) {
        super();
        this.name = name;
        this.price = price;
        this.ingredients = ingredients;
        this.producer = producer;
    }

    public TestProduct(String name, int price, String[] ingredients, TestOrg producer) {
        super();
        this.name = name;
        this.price = price;
        this.ingredients = Lists.newArrayList(ingredients);
        this.producer = producer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }
    
    public TestOrg getProducer() {
        return producer;
    }
    
    public void setProducer(TestOrg producer) {
        this.producer = producer;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((ingredients == null) ? 0 : ingredients.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + price;
        result = prime * result
                + ((producer == null) ? 0 : producer.hashCode());
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
        TestProduct other = (TestProduct) obj;
        if (ingredients == null) {
            if (other.ingredients != null)
                return false;
        } else if (!ingredients.equals(other.ingredients))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (price != other.price)
            return false;
        if (producer == null) {
            if (other.producer != null)
                return false;
        } else if (!producer.equals(other.producer))
            return false;
        return true;
    }

}
