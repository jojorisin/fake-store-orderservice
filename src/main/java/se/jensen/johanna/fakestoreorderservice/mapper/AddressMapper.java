package se.jensen.johanna.fakestoreorderservice.mapper;

import org.mapstruct.Mapper;
import se.jensen.johanna.fakestoreorderservice.dto.AddressRequest;
import se.jensen.johanna.fakestoreorderservice.model.ShippingAddress;

@Mapper(componentModel = "spring")
public interface AddressMapper {

  default ShippingAddress toShippingAddress(AddressRequest addressRequest) {
    return ShippingAddress.create(
        addressRequest.firstName(),
        addressRequest.lastName(),
        addressRequest.co(),
        addressRequest.streetName(),
        addressRequest.streetName2(),
        addressRequest.postalCode(),
        addressRequest.city(),
        addressRequest.country()
    );
  }

}
