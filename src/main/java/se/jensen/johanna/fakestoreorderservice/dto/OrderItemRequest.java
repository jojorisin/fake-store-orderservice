package se.jensen.johanna.fakestoreorderservice.dto;

import java.util.UUID;

public record OrderItemRequest(
    UUID productId,
    Integer quantity
) {

}
