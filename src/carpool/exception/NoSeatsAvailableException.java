package carpool.exception;

public class NoSeatsAvailableException extends Exception {
    public NoSeatsAvailableException(String message) {
        super(message);
    }
}
