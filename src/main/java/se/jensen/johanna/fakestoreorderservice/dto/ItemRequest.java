package se.jensen.johanna.fakestoreorderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record ItemRequest(
    @NotNull
    UUID productId,
    @NotBlank(message = "Title is required")
    String title,
    @NotNull
    @Positive
    BigDecimal pricePerItem,
    @NotNull
    @Positive
    Integer quantity

) {

}
