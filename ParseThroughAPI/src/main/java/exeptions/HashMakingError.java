package exeptions;

public class HashMakingError extends RuntimeException {
    public HashMakingError(String message) {
        super(message);
    }

    public HashMakingError(String message, Throwable cause) {}
}
