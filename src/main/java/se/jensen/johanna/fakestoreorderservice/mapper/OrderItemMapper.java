package se.jensen.johanna.fakestoreorderservice.mapper;

import org.mapstruct.Mapper;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartItemDTO;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

  default OrderItem toOrderItem(CheckoutCartItemDTO cartItemDTO) {
    return OrderItem.create(
        cartItemDTO.productId(),
        cartItemDTO.title(),
        cartItemDTO.pricePerItem(),
        cartItemDTO.quantity()
    );
  }


  CartItemRequest toCartItemRequest(CheckoutCartItemDTO itemDTO);


}
