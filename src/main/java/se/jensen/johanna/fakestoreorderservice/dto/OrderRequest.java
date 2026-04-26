package se.jensen.johanna.fakestoreorderservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record OrderRequest(
    @NotNull
    Set<OrderItemRequest> orderItemRequests,
    @NotNull
    AddressRequest addressRequest
) {

}
