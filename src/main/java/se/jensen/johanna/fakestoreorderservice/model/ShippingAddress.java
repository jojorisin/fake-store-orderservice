package se.jensen.johanna.fakestoreorderservice.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Embeddable
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ShippingAddress {

  private String firstName;

  private String lastName;

  private String co;

  private String streetName;


  private String streetName2;

  @Pattern(regexp = "\\d{5}")
  private String postalCode;

  private String city;

  private String country;

  public static ShippingAddress create(String firstName, String lastName, String co,
      String streetName, String streetName2, String postalCode, String city, String country) {
    return ShippingAddress.builder()
        .firstName(firstName)
        .lastName(lastName)
        .co(co)
        .streetName(streetName)
        .streetName2(streetName2)
        .postalCode(postalCode)
        .city(city)
        .country(country)
        .build();
  }


}
