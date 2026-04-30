package se.jensen.johanna.fakestoreorderservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ReservationRequest(
    @NotNull(message = "Please add items to cart.")
    Set<CartItemRequest> cartItemRequests

) {

}
