package se.jensen.johanna.fakestoreorderservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.OrderItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.ProductDTO;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

  @Mapping(target = "order", ignore = true)
  @Mapping(target = "pricePerItem", source = "productDTO.price")
  @Mapping(target = "quantity", source = "quantity")
  OrderItem toOrderItem(ProductDTO productDTO, Integer quantity);

  CartItemRequest toCartItemRequest(OrderItemRequest request);

}
