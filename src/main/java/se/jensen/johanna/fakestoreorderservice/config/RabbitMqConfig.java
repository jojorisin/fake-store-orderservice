package se.jensen.johanna.fakestoreorderservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("local")
@Configuration
public class RabbitMqConfig {

  @Value("${app.exchange.order-exchange}")
  private String orderExchange;

  @Bean
  public TopicExchange orderExchange() {
    return new TopicExchange(orderExchange);
  }

}
