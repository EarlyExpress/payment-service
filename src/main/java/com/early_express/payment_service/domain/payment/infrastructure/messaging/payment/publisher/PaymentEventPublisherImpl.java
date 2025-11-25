package com.early_express.payment_service.domain.payment.infrastructure.messaging.payment.publisher;

import com.early_express.payment_service.domain.payment.domain.messaging.PaymentEventPublisher;
import com.early_express.payment_service.domain.payment.domain.messaging.PaymentRefundFailedEventData;
import com.early_express.payment_service.domain.payment.domain.messaging.PaymentRefundedEventData;
import com.early_express.payment_service.domain.payment.infrastructure.messaging.payment.event.PaymentRefundFailedEvent;
import com.early_express.payment_service.domain.payment.infrastructure.messaging.payment.event.PaymentRefundedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Payment Event Publisher 구현체
 * Kafka로 이벤트 발행 (토픽 분리 패턴)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisherImpl implements PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 토픽 설정 (application.yml에서 주입)
    @Value("${spring.kafka.topic.payment-refunded:payment-refunded}")
    private String paymentRefundedTopic;

    @Value("${spring.kafka.topic.payment-refund-failed:payment-refund-failed}")
    private String paymentRefundFailedTopic;

    @Override
    public void publishPaymentRefunded(PaymentRefundedEventData data) {
        PaymentRefundedEvent event = PaymentRefundedEvent.from(data);

        sendEvent(paymentRefundedTopic, event.getOrderId(), event);

        log.info("PaymentRefundedEvent 발행 완료 - topic: {}, orderId: {}, refundAmount: {}",
                paymentRefundedTopic, data.getOrderId(), data.getRefundAmount());
    }

    @Override
    public void publishPaymentRefundFailed(PaymentRefundFailedEventData data) {
        PaymentRefundFailedEvent event = PaymentRefundFailedEvent.from(data);

        sendEvent(paymentRefundFailedTopic, event.getOrderId(), event);

        log.info("PaymentRefundFailedEvent 발행 완료 - topic: {}, orderId: {}",
                paymentRefundFailedTopic, data.getOrderId());
    }

    /**
     * Kafka로 이벤트 발행 (공통 헬퍼 메서드)
     */
    private void sendEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("이벤트 발행 실패 - topic: {}, key: {}, eventType: {}, error: {}",
                        topic, key, event.getClass().getSimpleName(), ex.getMessage());
            } else {
                log.debug("이벤트 발행 성공 - topic: {}, partition: {}, offset: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}