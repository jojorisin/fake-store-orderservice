package se.jensen.johanna.fakestoreorderservice.dto;

import java.util.List;

public record CheckoutCartResponse(
    List<CheckoutCartItemDTO> checkoutCart
) {

}
