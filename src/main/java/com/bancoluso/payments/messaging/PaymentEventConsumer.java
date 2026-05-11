package com.bancoluso.payments.messaging;

import com.bancoluso.payments.dto.PaymentEventDto;
import com.bancoluso.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    @KafkaListener(
            topics = "${payments.kafka.topic}",
            groupId = "${payments.kafka.group-id}",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload PaymentEventDto event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.debug("Received payment event [referenceId={}] from partition [{}] offset [{}]",
                event.getReferenceId(), partition, offset);

        paymentService.ingestEvent(event);
    }

}
