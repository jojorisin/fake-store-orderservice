package se.jensen.johanna.fakestoreorderservice.dto.event;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderConfirmedPaidEvent(
    @NotNull
    UUID orderId
) {

}
