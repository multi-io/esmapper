package de.olafklischat.esmapper;

public class CascadeSpec {
    
    public static final CascadeSpec NO_CASCADE = new CascadeSpec(false);
    public static final CascadeSpec FULL_CASCADE = new CascadeSpec(true);
    
    private boolean cascade;
    
    public CascadeSpec(boolean cascade) {
        super();
        this.cascade = cascade;
    }

    public boolean isCascade() {
        return cascade;
    }

    public void setCasecade(boolean casecade) {
        this.cascade = casecade;
    }

}
