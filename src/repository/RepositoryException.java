package repository;

/**
 * Raised when the underlying store cannot be read or written.
 *
 * Unchecked on purpose: the UI reports it to the user, it is not something the
 * service layer can meaningfully recover from.
 */
public class RepositoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
