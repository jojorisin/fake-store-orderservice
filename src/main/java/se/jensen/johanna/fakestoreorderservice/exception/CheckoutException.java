package se.jensen.johanna.fakestoreorderservice.exception;

import se.jensen.johanna.fakestoreorderservice.exception.domain.DomainException;

public class CheckoutException extends DomainException {

  public CheckoutException(String message) {
    super(message, ErrorCode.CHECKOUT_ERROR);
  }


}
