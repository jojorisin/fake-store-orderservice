package se.jensen.johanna.fakestoreorderservice.mapper;

import org.mapstruct.Mapper;
import se.jensen.johanna.fakestoreorderservice.dto.AddressRequest;
import se.jensen.johanna.fakestoreorderservice.model.ShippingAddress;

@Mapper(componentModel = "spring")
public interface AddressMapper {

  ShippingAddress toShippingAddress(AddressRequest addressRequest);

}
