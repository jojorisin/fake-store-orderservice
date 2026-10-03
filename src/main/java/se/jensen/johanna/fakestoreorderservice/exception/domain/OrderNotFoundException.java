package se.jensen.johanna.fakestoreorderservice.exception.domain;

import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

public class OrderNotFoundException extends DomainException {

  public OrderNotFoundException(String message) {
    super(message, ErrorCode.ORDER_NOT_FOUND);
  }
}
