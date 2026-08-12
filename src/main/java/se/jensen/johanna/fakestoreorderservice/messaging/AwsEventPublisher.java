package se.jensen.johanna.fakestoreorderservice.messaging;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
@RequiredArgsConstructor
@Slf4j
public class AwsEventPublisher implements OrderEventPublisher {

  private final SqsTemplate sqsTemplate;

  @Value("${app.queues.confirm-reservation}")
  private String confirmReservationQueue;

  @Override
  public void publishConfirmReservationEvent(UUID orderId) {
    log.info("Publishing confirm reservation event for order: {}...", orderId);

    sqsTemplate.send(to -> to.queue(confirmReservationQueue).payload(orderId));
    log.info("Confirm reservation event published for order: {}", orderId);
  }


}
