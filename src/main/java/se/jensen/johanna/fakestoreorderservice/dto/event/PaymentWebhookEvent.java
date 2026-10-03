package se.jensen.johanna.fakestoreorderservice.dto.event;

import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentStatus;

public record PaymentWebhookEvent(
    PaymentStatus paymentStatus,
    String orderId,
    String paymentReference
) {


}
