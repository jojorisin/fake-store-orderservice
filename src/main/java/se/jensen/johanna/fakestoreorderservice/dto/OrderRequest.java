package se.jensen.johanna.fakestoreorderservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderRequest(
    @NotNull
    List<ItemRequest> itemRequests,
    @NotNull
    AddressRequest addressRequest
) {

}
