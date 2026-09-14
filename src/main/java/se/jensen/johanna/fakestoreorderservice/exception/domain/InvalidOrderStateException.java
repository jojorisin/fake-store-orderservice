package se.jensen.johanna.fakestoreorderservice.exception.domain;

import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

public class InvalidOrderStateException extends DomainException {


  public InvalidOrderStateException(String message) {
    super(message, ErrorCode.INVALID_ORDER_STATE);
  }

}
