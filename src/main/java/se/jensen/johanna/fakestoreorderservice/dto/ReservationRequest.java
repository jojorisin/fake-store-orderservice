package se.jensen.johanna.fakestoreorderservice.dto;

import java.util.Set;

public record ReservationRequest(
    Set<CartItemRequest> cartItemRequests

) {

}
