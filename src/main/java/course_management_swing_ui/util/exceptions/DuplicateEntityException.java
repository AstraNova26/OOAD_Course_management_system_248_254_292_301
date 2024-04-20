package course_management_swing_ui.util.exceptions;

/**
 * @Overview A class denoting that there are two or more entity instances that must be unique.
 */
public class DuplicateEntityException extends Exception {
    public DuplicateEntityException() {
        super();
    }

    public DuplicateEntityException(String message) {
        super(message);
    }
}
