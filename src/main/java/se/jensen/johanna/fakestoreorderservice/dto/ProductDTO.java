package se.jensen.johanna.fakestoreorderservice.dto;

public record ProductDTO(
    Long id,
    String title,
    Integer price,
    String description,
    String image
) {

}
