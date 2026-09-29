package se.jensen.johanna.fakestoreorderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutCartItemDTO(
    UUID productId,
    Integer quantity,
    BigDecimal pricePerItem,
    String title
) {

}
