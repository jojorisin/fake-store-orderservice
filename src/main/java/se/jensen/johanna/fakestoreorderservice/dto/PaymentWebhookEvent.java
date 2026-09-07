package se.jensen.johanna.fakestoreorderservice.dto;

import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentEventType;

public record PaymentWebhookEvent(
    PaymentEventType eventType,
    String orderId,
    String paymentReference
) {


}
