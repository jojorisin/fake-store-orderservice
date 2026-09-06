package se.jensen.johanna.fakestoreorderservice.service;

import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentType;

public interface PaymentProvider {

  public boolean supports(PaymentType paymentMethod);

  public PaymentType getPaymentType();

  CheckoutResponse createCheckoutSession(Order order, String email);
}
