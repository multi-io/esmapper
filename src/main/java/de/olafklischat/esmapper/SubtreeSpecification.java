package de.olafklischat.esmapper;

import java.util.List;

public class SubtreeSpecification {

    private List<Rule> rules;

    public static class Rule {
        String propertyNamePattern;
        int depth;
    }
    
    
    
}
