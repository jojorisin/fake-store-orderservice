package se.jensen.johanna.fakestoreorderservice.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderPaidEvent(
    @NotBlank
    String paymentReference,
    @NotNull
    UUID orderId
) {

}
