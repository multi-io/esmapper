package de.olafklischat.esmapper;

public class CascadeSpec {
    
    public static final CascadeSpec NO_CASCADE = new CascadeSpec(false);
    public static final CascadeSpec FULL_CASCADE = new CascadeSpec(true);
    
    private boolean casecade;
    
    public CascadeSpec(boolean casecade) {
        super();
        this.casecade = casecade;
    }

    public boolean isCasecade() {
        return casecade;
    }

    public void setCasecade(boolean casecade) {
        this.casecade = casecade;
    }

}
