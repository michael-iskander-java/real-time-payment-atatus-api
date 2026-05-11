package com.bancoluso.payments.integration;

import com.bancoluso.payments.dto.PaymentEventDto;
import com.bancoluso.payments.entity.Payment;
import com.bancoluso.payments.entity.PaymentStatus;
import com.bancoluso.payments.repository.PaymentRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static com.bancoluso.payments.common.TestConstants.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class PaymentIntegrationTest {

    @Container
    @ServiceConnection
    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    private static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.1");

    @DynamicPropertySource
    private static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void cleanDb() {
        paymentRepository.deleteAll();
    }

    @Test
    void postEvent_newPayment_returns202AndPersists() throws Exception {
        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PENDING, EVENT_TIME));

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(paymentRepository.findByReferenceId(REFERENCE_ID))
                        .isPresent()
                        .get()
                        .satisfies(p -> {
                            assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
                            assertThat(p.getAmount()).isEqualByComparingTo("1500.00");
                            assertThat(p.getCurrency()).isEqualTo("EUR");
                        }));

    }

    @Test
    void postEvent_trueDuplicate_returns202NoUpdate() throws Exception {
        PaymentEventDto request = buildRequest(REFERENCE_ID, PaymentStatus.PENDING, EVENT_TIME);

        postEvent(request);
        postEvent(request);

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(paymentRepository.count()).isEqualTo(1);
            assertThat(paymentRepository.findByReferenceId(REFERENCE_ID).get().getStatus())
                    .isEqualTo(PaymentStatus.PENDING);
        });
    }

    @Test
    void postEvent_newerStatus_updatesPayment() throws Exception {
        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PENDING, EVENT_TIME));

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(paymentRepository.findByReferenceId(REFERENCE_ID)).isPresent());

        Instant updatedAt = paymentRepository.findByReferenceId(REFERENCE_ID).get().getUpdatedAt();


        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PROCESSING, NEWER_EVENT_TIME));

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            Payment payment = paymentRepository.findByReferenceId(REFERENCE_ID).get();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
            assertThat(payment.getUpdatedAt()).isAfter(updatedAt);
        });
    }

    @Test
    void postEvent_staleEvent_statusUnchanged() throws Exception {
        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PROCESSING, EVENT_TIME));

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(paymentRepository.findByReferenceId(REFERENCE_ID)).isPresent());

        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PENDING, STALE_EVENT_TIME));

        Thread.sleep(3000);
        assertThat(paymentRepository.findByReferenceId(REFERENCE_ID).get().getStatus())
                .isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    void postEvent_financialFieldsImmutable() throws Exception {
        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PENDING, EVENT_TIME));

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(paymentRepository.findByReferenceId(REFERENCE_ID)).isPresent());

        PaymentEventDto update = buildRequest(REFERENCE_ID, PaymentStatus.PROCESSING, NEWER_EVENT_TIME);
        update.setAmount(new BigDecimal("9999.00"));
        postEvent(update);

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(paymentRepository.findByReferenceId(REFERENCE_ID).get().getStatus())
                        .isEqualTo(PaymentStatus.PROCESSING));

        assertThat(paymentRepository.findByReferenceId(REFERENCE_ID).get().getAmount())
                .isEqualByComparingTo("1500.00");
    }


    @Test
    void postEvent_missingFields_returns400() throws Exception {
        mockMvc.perform(post(EVENT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void postEvent_malformedJson_returns400() throws Exception {
        mockMvc.perform(post(EVENT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPaymentStatus_existingPayment_returns200() throws Exception {
        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PENDING, EVENT_TIME));

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                mockMvc.perform(get(STATUS_URL, REFERENCE_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.referenceId", is(REFERENCE_ID)))
                        .andExpect(jsonPath("$.status", is("PENDING"))));
    }

    @Test
    void getPaymentStatus_unknownId_returns404() throws Exception {
        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PENDING, EVENT_TIME));

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                mockMvc.perform(get(STATUS_URL, "unknown-reference"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status", is(404))));
    }

    @Test
    void getPaymentStatus_reflectsLatestStatus() throws Exception {
        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PENDING, EVENT_TIME));
        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.PROCESSING, EVENT_TIME.plusSeconds(30)));
        postEvent(buildRequest(REFERENCE_ID, PaymentStatus.SETTLED, EVENT_TIME.plusSeconds(60)));

        await().atMost(CONSUMER_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                mockMvc.perform(get("/v1/payments/{referenceId}/status", REFERENCE_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status", is("SETTLED"))));
    }

    private void postEvent(PaymentEventDto request) throws Exception {
        mockMvc.perform(post(EVENT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }
}
