package se.jensen.johanna.fakestoreorderservice.dto;

import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

public record CheckoutResponse(
    String checkoutUrl,
    String paymentReference,
    PaymentProviderType paymentProviderType
) {

}
