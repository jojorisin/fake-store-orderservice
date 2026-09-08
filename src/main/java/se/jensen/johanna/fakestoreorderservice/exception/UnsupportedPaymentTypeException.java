package se.jensen.johanna.fakestoreorderservice.exception;

import org.springframework.http.HttpStatus;
import se.jensen.johanna.fakestoreorderservice.exception.domain.DomainException;

public class UnsupportedPaymentTypeException extends DomainException {

  public UnsupportedPaymentTypeException(String message, ErrorCode errorCode, HttpStatus status) {
    super(message, errorCode);
  }
}
