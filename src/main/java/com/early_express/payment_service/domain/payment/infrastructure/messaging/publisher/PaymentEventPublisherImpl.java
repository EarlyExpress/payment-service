package com.early_express.payment_service.domain.payment.infrastructure.messaging.publisher;

import com.early_express.payment_service.domain.payment.domain.messaging.*;
import com.early_express.payment_service.domain.payment.infrastructure.messaging.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Payment Event Publisher 구현체
 * Kafka로 이벤트 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisherImpl implements PaymentEventPublisher {

    @Value("${spring.application.name}")
    private String applicationName;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 이벤트 토픽 이름 생성
     */
    private String getEventsTopic() {
        return applicationName + "-events";
    }

    @Override
    public void publishPaymentRefunded(PaymentRefundedEventData data) {
        PaymentRefundedEvent event = PaymentRefundedEvent.from(data);
        publishEvent(getEventsTopic(), event.getOrderId(), event);

        log.info("PaymentRefundedEvent 발행 완료 - orderId: {}, refundAmount: {}",
                data.getOrderId(), data.getRefundAmount());
    }

    @Override
    public void publishPaymentRefundFailed(PaymentRefundFailedEventData data) {
        PaymentRefundFailedEvent event = PaymentRefundFailedEvent.from(data);
        publishEvent(getEventsTopic(), event.getOrderId(), event);

        log.info("PaymentRefundFailedEvent 발행 완료 - orderId: {}",
                data.getOrderId());
    }

    /**
     * Kafka로 이벤트 발행 (공통 메서드)
     */
    private void publishEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("이벤트 발행 실패 - topic: {}, key: {}, event: {}",
                        topic, key, event.getClass().getSimpleName(), ex);
            } else {
                log.debug("이벤트 발행 성공 - topic: {}, partition: {}, offset: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
