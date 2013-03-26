package de.olafklischat.esmapper;

public class EntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -3511148244879158767L;

    public EntityNotFoundException() {
        super();
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(Throwable cause) {
        super(cause);
    }
    
}
