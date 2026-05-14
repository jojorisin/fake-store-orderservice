package se.jensen.johanna.fakestoreorderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StripeEventDTO(
    Detail detail
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Detail(
      Data data
  ) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(
      @JsonProperty("object") StripeObject stripeObject
  ) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StripeObject(
      @JsonProperty("id") String sessionId,
      @JsonProperty("payment_status") String paymentStatus,
      @JsonProperty("customer_email") String userEmail,
      @JsonProperty("amount_total") Long totalAmount,
      Metadata metadata
  ) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Metadata(
      String orderId
  ) {

  }
}
