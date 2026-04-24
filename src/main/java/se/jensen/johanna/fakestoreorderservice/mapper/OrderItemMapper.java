package se.jensen.johanna.fakestoreorderservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import se.jensen.johanna.fakestoreorderservice.dto.ItemRequest;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

  @Mapping(target = "order", ignore = true)
  OrderItem toOrderItem(ItemRequest itemRequest);

}
