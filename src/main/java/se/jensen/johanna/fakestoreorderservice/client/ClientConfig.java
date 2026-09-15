package se.jensen.johanna.fakestoreorderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

  @Value("${product-service-url}")
  private String productServiceBaseUrl;

  @Value("${inventory-service-url}")
  private String inventoryServiceBaseUrl;

  @Bean
  ProductClient productClient(RestClient.Builder builder) {
    RestClient restClient = builder.baseUrl(productServiceBaseUrl).build();
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build();

    return factory.createClient(ProductClient.class);
  }

  @Bean
  InventoryClient inventoryClient(RestClient.Builder builder) {
    RestClient restClient = builder.baseUrl(inventoryServiceBaseUrl).build();
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build();

    return factory.createClient(InventoryClient.class);
  }
}
