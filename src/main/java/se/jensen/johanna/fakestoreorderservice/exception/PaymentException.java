package se.jensen.johanna.fakestoreorderservice.exception;

public class PaymentException extends DomainException {

  public PaymentException(String message) {
    super(message, ErrorType.PAYMENT_ERROR);
  }
}
