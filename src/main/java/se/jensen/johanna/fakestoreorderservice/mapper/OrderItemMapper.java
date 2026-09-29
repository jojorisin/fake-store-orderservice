package se.jensen.johanna.fakestoreorderservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartItemDTO;
import se.jensen.johanna.fakestoreorderservice.dto.ProductDTO;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

  @Mapping(target = "order", ignore = true)
  @Mapping(target = "pricePerItem", source = "productDTO.price")
  @Mapping(target = "quantity", source = "quantity")
  @Mapping(target = "orderItemId", ignore = true)
  OrderItem toOrderItem(ProductDTO productDTO, Integer quantity);

  @Mapping(target = "order", ignore = true)
  @Mapping(target = "orderItemId", ignore = true)
  OrderItem toOrderItem(CheckoutCartItemDTO cartItemDTO);


  CartItemRequest toCartItemRequest(CheckoutCartItemDTO itemDTO);


}
