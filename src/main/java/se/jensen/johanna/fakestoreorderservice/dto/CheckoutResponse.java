package se.jensen.johanna.fakestoreorderservice.dto;

public record CheckoutResponse(
    String stripeUrl,
    String sessionId
) {

}
