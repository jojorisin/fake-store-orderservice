package se.jensen.johanna.fakestoreorderservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

public record OrderRequest(
    @NotNull(message = "Payment method is required.") PaymentProviderType paymentType,
    @NotNull(message = "Cart is empty")
    Set<CartItemRequest> itemRequests,
    @NotNull(message = "Address is required.")
    AddressRequest addressRequest
) {

}
