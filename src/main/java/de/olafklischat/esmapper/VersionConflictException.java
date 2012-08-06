package de.olafklischat.esmapper;

public class VersionConflictException extends RuntimeException {

    private static final long serialVersionUID = 3513334989150226204L;

    public VersionConflictException() {
    }

    public VersionConflictException(String message) {
        super(message);
    }

    public VersionConflictException(Throwable cause) {
        super(cause);
    }

    public VersionConflictException(String message, Throwable cause) {
        super(message, cause);
    }

}
