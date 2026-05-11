package com.bancoluso.payments.messaging;


import com.bancoluso.payments.dto.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    @Value("${payments.kafka.topic}")
    private String topic;

    public void produce(PaymentEventDto event) {
        CompletableFuture<SendResult<String, PaymentEventDto>> future =
                kafkaTemplate.send(topic, event.getReferenceId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish payment event [referenceId={}] to topic [{}]: {}",
                        event.getReferenceId(), topic, ex.getMessage(), ex);
            } else {
                log.debug("Published payment event [referenceId={}] to topic [{}] partition [{}] offset [{}]",
                        event.getReferenceId(),
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
