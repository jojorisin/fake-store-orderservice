package se.jensen.johanna.fakestoreorderservice.exception.domain;

import lombok.Getter;
import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

@Getter
public abstract class DomainException extends RuntimeException {

  private final ErrorCode errorCode;

  public DomainException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }


}
