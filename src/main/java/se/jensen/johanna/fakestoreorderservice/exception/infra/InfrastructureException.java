package se.jensen.johanna.fakestoreorderservice.exception.infra;

import lombok.Getter;
import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

@Getter
public abstract class InfrastructureException extends RuntimeException {

  private final ErrorCode errorCode;

  public InfrastructureException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public InfrastructureException(String message, ErrorCode errorCode,
      Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

}
