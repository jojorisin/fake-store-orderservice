package se.jensen.johanna.fakestoreorderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

  @Value("${product-service-url}")
  private String productServiceBaseUrl;

  @Value("${inventory-service-url}")
  private String inventoryServiceBaseUrl;

  @Value("${cart-service-url}")
  private String cartServiceBaseUrl;

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

  @Bean
  CartClient cartClient(RestClient.Builder builder) {
    RestClient restClient = builder.baseUrl(cartServiceBaseUrl)
        .requestInterceptor((request, body, execution) -> {
          Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
          if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            request.getHeaders().setBearerAuth(jwtAuth.getToken().getTokenValue());
          }
          return execution.execute(request, body);
        }).build();
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();

    return factory.createClient(CartClient.class);
  }
}
