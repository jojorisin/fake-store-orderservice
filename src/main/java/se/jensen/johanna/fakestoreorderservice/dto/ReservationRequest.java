package se.jensen.johanna.fakestoreorderservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReservationRequest(
    @NotNull
    List<CartItemRequest> cartItemRequests,
    @NotNull
    UUID orderId
) {

}
