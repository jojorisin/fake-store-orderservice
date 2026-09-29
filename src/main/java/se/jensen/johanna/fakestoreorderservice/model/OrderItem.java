package se.jensen.johanna.fakestoreorderservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "order_items")
@Getter
@Builder
public class OrderItem {

  @Id
  @GeneratedValue
  private UUID orderItemId;

  @NotNull
  private UUID productId;

  @NotNull
  private String title;

  @NotNull
  private BigDecimal pricePerItem;

  @Min(1)
  private int quantity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  @NotNull
  private Order order;

  public static OrderItem create(UUID productId, String title, BigDecimal pricePerItem,
      int quantity) {
    if (pricePerItem.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(
          "Invalid price for order item. price: " + pricePerItem + ", product id: " + productId);
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException(
          "Invalid quantity. quantity: " + quantity + ", product id: " + productId);
    }
    return OrderItem.builder()
        .productId(productId)
        .title(title)
        .pricePerItem(pricePerItem)
        .quantity(quantity)
        .build();
  }

  public BigDecimal calculateTotalPrice() {
    return pricePerItem.multiply(BigDecimal.valueOf(quantity));
  }

  public void giveParent(Order order) {
    this.order = order;
  }

}
