package se.jensen.johanna.fakestoreorderservice.exception.domain;

import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

public class ProductNotFound extends DomainException {

  public ProductNotFound(String message) {
    super(message, ErrorCode.PRODUCT_NOT_FOUND);
  }
}
