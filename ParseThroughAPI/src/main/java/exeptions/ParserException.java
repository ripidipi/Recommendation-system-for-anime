package exeptions;

public class ParserException extends RuntimeException {
  public ParserException(String message, Throwable cause) {
    super(message, cause);
  }
  public ParserException(String message) {
    super(message);
  }
}
