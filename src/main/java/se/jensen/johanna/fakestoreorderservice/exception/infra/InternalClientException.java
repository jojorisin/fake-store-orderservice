package se.jensen.johanna.fakestoreorderservice.exception.infra;

import lombok.Getter;
import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

@Getter
public class InternalClientException extends InfrastructureException {

  public InternalClientException(String message) {
    super(message, ErrorCode.SERVICE_ERROR);
  }

  public InternalClientException(String message, Throwable cause) {
    super(message, ErrorCode.SERVICE_ERROR, cause);
  }
}
