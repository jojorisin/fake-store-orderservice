package se.jensen.johanna.fakestoreorderservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
@Profile("!local")
public class SqsConfig {

  @Bean
  public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper) {
    SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
    converter.setObjectMapper(objectMapper);
    converter.setPayloadTypeMapper(msg -> null);

    return SqsTemplate.builder()
        .sqsAsyncClient(sqsAsyncClient)
        .messageConverter(converter)
        .build();
  }

}
