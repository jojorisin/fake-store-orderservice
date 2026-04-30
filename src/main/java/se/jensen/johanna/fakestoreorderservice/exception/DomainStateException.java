package se.jensen.johanna.fakestoreorderservice.exception;

public class DomainStateException extends DomainException {

  public DomainStateException(String message) {
    super(message, ErrorType.ILLEGAL_STATE);
  }
}
