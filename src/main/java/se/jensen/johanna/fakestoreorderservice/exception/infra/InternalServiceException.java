package se.jensen.johanna.fakestoreorderservice.exception.infra;

import lombok.Getter;
import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

@Getter
public class InternalServiceException extends InfrastructureException {

  public InternalServiceException(String message) {
    super(message, ErrorCode.INTERNAL_SERVICE_ERROR);
  }

  public InternalServiceException(String message, Throwable cause) {
    super(message, ErrorCode.INTERNAL_SERVICE_ERROR, cause);
  }
}
