package se.jensen.johanna.fakestoreorderservice.exception;

public class UnsupportedPaymentTypeException extends DomainException {

  public UnsupportedPaymentTypeException(String message) {
    super(message, ErrorType.UNSUPPORTED_PAYMENT_TYPE);
  }
}
