package se.jensen.johanna.fakestoreorderservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class DomainException extends RuntimeException {

  private final ErrorType errorCode;
  private final HttpStatus status;

  public DomainException(String message, ErrorType errorCode) {
    super(message);
    this.errorCode = errorCode;
    this.status = errorCode.getStatus();
  }
}
