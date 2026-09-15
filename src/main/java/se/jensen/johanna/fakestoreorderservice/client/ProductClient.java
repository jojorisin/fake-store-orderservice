package se.jensen.johanna.fakestoreorderservice.client;

import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import se.jensen.johanna.fakestoreorderservice.dto.ProductBatchResponse;

public interface ProductClient {

  @PostExchange("/internal/products/batch")
  ProductBatchResponse getProductBatch(@RequestBody Set<UUID> productIds);

}
