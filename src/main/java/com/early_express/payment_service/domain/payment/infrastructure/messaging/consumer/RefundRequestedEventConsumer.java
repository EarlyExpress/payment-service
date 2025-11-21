package com.early_express.payment_service.domain.payment.infrastructure.messaging.consumer;

import com.early_express.payment_service.domain.payment.application.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 환불 요청 이벤트 Consumer
 * Order Service에서 발행한 환불 요청을 수신하여 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRequestedEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    /**
     * 환불 요청 이벤트 수신
     * Topic: order-service-events
     * Group: payment-service-refund-group
     */
    @KafkaListener(
            topics = "${kafka.topic.order-events:order-service-events}",
            groupId = "${kafka.consumer.group-id:payment-service-refund-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRefundRequested(
            @Payload Map<String, Object> eventMap,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            // Map에서 필요한 정보 추출
            String eventType = (String) eventMap.get("eventType");

            // RefundRequestedEvent가 아니면 무시
            if (!"RefundRequestedEvent".equals(eventType)) {
                log.debug("다른 이벤트 타입 무시 - eventType: {}", eventType);
                acknowledgment.acknowledge();
                return;
            }

            String paymentId = (String) eventMap.get("paymentId");
            String orderId = (String) eventMap.get("orderId");
            String refundReason = (String) eventMap.get("refundReason");

            log.info("환불 요청 이벤트 수신 - paymentId: {}, orderId: {}, partition: {}, offset: {}",
                    paymentId, orderId, partition, offset);

            // 기존 PaymentService의 cancelPayment 메서드 사용
            paymentService.cancelPayment(paymentId, orderId, refundReason);

            // 수동 커밋
            acknowledgment.acknowledge();

            log.info("환불 요청 처리 완료 - paymentId: {}, orderId: {}", paymentId, orderId);

        } catch (Exception e) {
            log.error("환불 요청 처리 실패 - eventMap: {}, error: {}",
                    eventMap, e.getMessage(), e);

            // 재시도를 위해 ACK 하지 않음
            throw e;
        }
    }
}