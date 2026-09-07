package se.jensen.johanna.fakestoreorderservice.dto;

public record CheckoutResponse(
    String checkoutUrl,
    String paymentReference
) {

}
