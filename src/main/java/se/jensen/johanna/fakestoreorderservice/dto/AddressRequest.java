package se.jensen.johanna.fakestoreorderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddressRequest(
    @NotBlank(message = "First name is required")
    String firstName,

    @NotBlank(message = "Last name is required")
    String lastName,

    String co,

    @NotBlank(message = "Street name is required")
    String streetName,

    String streetName2,

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "\\d{5}", message = "Invalid postal code format.")
    String postalCode,

    @NotBlank(message = "City is required")
    String city,

    @NotBlank(message = "Country is required")
    String country
) {

}
