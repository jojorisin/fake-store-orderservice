package se.jensen.johanna.fakestoreorderservice.exception;

import org.springframework.http.HttpStatus;
import se.jensen.johanna.fakestoreorderservice.exception.domain.DomainException;

public class DomainStateException extends DomainException {

  public DomainStateException(String message, ErrorCode errorCode, HttpStatus status) {
    super(message, errorCode, status);
  }

  public DomainStateException(String message, ErrorCode errorCode, HttpStatus status,
      Throwable cause) {
    super(message, errorCode, status, cause);
  }
}
