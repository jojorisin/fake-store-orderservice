package se.jensen.johanna.fakestoreorderservice.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
  ILLEGAL_STATE,
  CHECKOUT_ERROR,
  UNSUPPORTED_PAYMENT_TYPE,
  INVALID_WEBHOOK_SIGNATURE,
  PRODUCT_NOT_FOUND,
  INTERNAL_SERVICE_ERROR,
  PAYMENT_PROVIDER_ERROR,
  INVALID_ORDER_STATE;


}
