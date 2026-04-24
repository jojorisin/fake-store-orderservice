package se.jensen.johanna.fakestoreorderservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

  @NotNull
  private Integer quantity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  @NotNull
  private Order order;

  public BigDecimal calculateTotalPrice() {
    return this.pricePerItem.multiply(BigDecimal.valueOf(this.quantity));
  }

}
