package se.jensen.johanna.fakestoreorderservice.exception.infra;

import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

public class PaymentProviderException extends InfrastructureException {

  public PaymentProviderException(String message, Throwable cause) {
    super(message, ErrorCode.PAYMENT_PROVIDER_ERROR, cause);
  }
}
