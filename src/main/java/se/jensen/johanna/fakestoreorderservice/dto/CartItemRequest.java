package se.jensen.johanna.fakestoreorderservice.dto;

import java.util.UUID;

public record CartItemRequest(
    UUID productId,
    Integer quantity
) {

}
