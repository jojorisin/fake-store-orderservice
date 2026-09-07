package se.jensen.johanna.fakestoreorderservice.service;

import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.PaymentWebhookEvent;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

public interface PaymentProvider {

  public boolean supports(PaymentProviderType paymentMethod);

  public PaymentProviderType getPaymentType();

  CheckoutResponse createCheckoutSession(Order order, String email);

  public PaymentWebhookEvent parseWebhookEvent(String payload, String signature);
}
