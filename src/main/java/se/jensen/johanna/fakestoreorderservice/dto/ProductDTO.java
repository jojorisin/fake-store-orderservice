package se.jensen.johanna.fakestoreorderservice.dto;

import java.util.UUID;

public record ProductDTO(
    UUID productId,
    String title,
    Integer price,
    String description,
    String image
) {

}
