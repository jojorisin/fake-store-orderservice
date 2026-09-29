package se.jensen.johanna.fakestoreorderservice.exception.domain;

import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

public class EmptyCartException extends DomainException {

  public EmptyCartException(String message) {
    super(message, ErrorCode.EMPTY_CART);
  }
}
