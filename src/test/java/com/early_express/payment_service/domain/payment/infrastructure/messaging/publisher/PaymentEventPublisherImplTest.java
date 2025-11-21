package com.early_express.payment_service.domain.payment.infrastructure.messaging.publisher;

import com.early_express.payment_service.domain.payment.domain.messaging.PaymentRefundFailedEventData;
import com.early_express.payment_service.domain.payment.domain.messaging.PaymentRefundedEventData;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * PaymentEventPublisher 단위 테스트
 * Mockito를 사용하여 Kafka 의존성 없이 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventPublisher 단위 테스트")
class PaymentEventPublisherImplTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentEventPublisherImpl eventPublisher;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    /**
     * Mock SendResult 생성 헬퍼 메서드
     */
    private CompletableFuture<SendResult<String, Object>> createMockFuture() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();

        ProducerRecord<String, Object> producerRecord =
                new ProducerRecord<>("payment-service-events", "test-key", new Object());

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("payment-service-events", 0),
                0L, 0, 0L, 0, 0
        );

        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
        future.complete(sendResult);

        return future;
    }

    @Test
    @DisplayName("PaymentRefundedEvent 발행 - Kafka로 전송된다")
    void publishPaymentRefunded_Success() {
        // given
        ReflectionTestUtils.setField(eventPublisher, "applicationName", "payment-service");

        PaymentRefundedEventData eventData = PaymentRefundedEventData.builder()
                .paymentId("payment-123")
                .orderId("ORDER-001")
                .refundAmount(new BigDecimal("10000"))
                .totalRefundedAmount(new BigDecimal("10000"))
                .refundReason("고객 요청")
                .pgRefundId("refund-tx-123")
                .fullRefund(true)
                .refundedAt(LocalDateTime.now())
                .build();

        given(kafkaTemplate.send(anyString(), anyString(), any()))
                .willReturn(createMockFuture());

        // when
        eventPublisher.publishPaymentRefunded(eventData);

        // then
        verify(kafkaTemplate).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo("payment-service-events");
        assertThat(keyCaptor.getValue()).isEqualTo("ORDER-001");
        assertThat(eventCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("PaymentRefundFailedEvent 발행 - Kafka로 전송된다")
    void publishPaymentRefundFailed_Success() {
        // given
        ReflectionTestUtils.setField(eventPublisher, "applicationName", "payment-service");

        PaymentRefundFailedEventData eventData = PaymentRefundFailedEventData.builder()
                .paymentId("payment-456")
                .orderId("ORDER-002")
                .requestedRefundAmount(new BigDecimal("20000"))
                .errorMessage("PG 통신 오류")
                .failedAt(LocalDateTime.now())
                .build();

        given(kafkaTemplate.send(anyString(), anyString(), any()))
                .willReturn(createMockFuture());

        // when
        eventPublisher.publishPaymentRefundFailed(eventData);

        // then
        verify(kafkaTemplate).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo("payment-service-events");
        assertThat(keyCaptor.getValue()).isEqualTo("ORDER-002");
        assertThat(eventCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("여러 이벤트 연속 발행 - 모두 전송된다")
    void publishMultipleEvents_Success() {
        // given
        ReflectionTestUtils.setField(eventPublisher, "applicationName", "payment-service");

        PaymentRefundedEventData event1 = PaymentRefundedEventData.builder()
                .paymentId("payment-001")
                .orderId("ORDER-001")
                .refundAmount(new BigDecimal("5000"))
                .totalRefundedAmount(new BigDecimal("5000"))
                .refundReason("부분 환불 1")
                .pgRefundId("refund-1")
                .fullRefund(false)
                .refundedAt(LocalDateTime.now())
                .build();

        PaymentRefundedEventData event2 = PaymentRefundedEventData.builder()
                .paymentId("payment-001")
                .orderId("ORDER-001")
                .refundAmount(new BigDecimal("5000"))
                .totalRefundedAmount(new BigDecimal("10000"))
                .refundReason("부분 환불 2")
                .pgRefundId("refund-2")
                .fullRefund(true)
                .refundedAt(LocalDateTime.now())
                .build();

        given(kafkaTemplate.send(anyString(), anyString(), any()))
                .willReturn(createMockFuture());

        // when
        eventPublisher.publishPaymentRefunded(event1);
        eventPublisher.publishPaymentRefunded(event2);

        // then
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("부분 환불 이벤트 발행 - fullRefund=false")
    void publishPartialRefund_Success() {
        // given
        ReflectionTestUtils.setField(eventPublisher, "applicationName", "payment-service");

        PaymentRefundedEventData eventData = PaymentRefundedEventData.builder()
                .paymentId("payment-789")
                .orderId("ORDER-003")
                .refundAmount(new BigDecimal("3000"))
                .totalRefundedAmount(new BigDecimal("3000"))
                .refundReason("부분 환불")
                .pgRefundId("refund-partial")
                .fullRefund(false)
                .refundedAt(LocalDateTime.now())
                .build();

        given(kafkaTemplate.send(anyString(), anyString(), any()))
                .willReturn(createMockFuture());

        // when
        eventPublisher.publishPaymentRefunded(eventData);

        // then
        verify(kafkaTemplate).send(anyString(), eq("ORDER-003"), any());
    }

    @Test
    @DisplayName("환불 실패 이벤트 - 에러 메시지 포함")
    void publishRefundFailed_WithErrorMessage() {
        // given
        ReflectionTestUtils.setField(eventPublisher, "applicationName", "payment-service");

        PaymentRefundFailedEventData eventData = PaymentRefundFailedEventData.builder()
                .paymentId("payment-error")
                .orderId("ORDER-ERROR")
                .requestedRefundAmount(new BigDecimal("50000"))
                .errorMessage("환불 한도 초과")
                .failedAt(LocalDateTime.now())
                .build();

        given(kafkaTemplate.send(anyString(), anyString(), any()))
                .willReturn(createMockFuture());

        // when
        eventPublisher.publishPaymentRefundFailed(eventData);

        // then
        verify(kafkaTemplate).send(anyString(), eq("ORDER-ERROR"), any());
    }

    @Test
    @DisplayName("전액 환불 이벤트 발행 - fullRefund=true")
    void publishFullRefund_Success() {
        // given
        ReflectionTestUtils.setField(eventPublisher, "applicationName", "payment-service");

        PaymentRefundedEventData eventData = PaymentRefundedEventData.builder()
                .paymentId("payment-full")
                .orderId("ORDER-FULL")
                .refundAmount(new BigDecimal("10000"))
                .totalRefundedAmount(new BigDecimal("10000"))
                .refundReason("전액 환불")
                .pgRefundId("refund-full")
                .fullRefund(true)
                .refundedAt(LocalDateTime.now())
                .build();

        given(kafkaTemplate.send(anyString(), anyString(), any()))
                .willReturn(createMockFuture());

        // when
        eventPublisher.publishPaymentRefunded(eventData);

        // then
        verify(kafkaTemplate).send(
                eq("payment-service-events"),
                eq("ORDER-FULL"),
                any()
        );
    }
}