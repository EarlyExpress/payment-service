package com.early_express.payment_service.domain.payment.infrastructure.messaging.order.consumer;

import com.early_express.payment_service.domain.payment.application.service.PaymentService;
import com.early_express.payment_service.domain.payment.infrastructure.messaging.order.event.RefundRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 환불 요청 이벤트 Consumer
 * Order Service에서 발행한 환불 요청을 수신하여 처리
 * Topic: refund-requested (토픽 분리 패턴)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRequestedEventConsumer {

    private final PaymentService paymentService;

    /**
     * 환불 요청 이벤트 수신
     * Topic: refund-requested
     */
    @KafkaListener(
            topics = "${spring.kafka.topic.refund-requested:refund-requested}",
            groupId = "${spring.kafka.consumer.group-id:payment-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRefundRequested(
            @Payload RefundRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("환불 요청 이벤트 수신 - paymentId: {}, orderId: {}, partition: {}, offset: {}",
                event.getPaymentId(), event.getOrderId(), partition, offset);

        try {
            // PaymentService의 cancelPayment 메서드 호출
            paymentService.cancelPayment(
                    event.getPaymentId(),
                    event.getOrderId(),
                    event.getRefundReason()
            );

            // 수동 커밋
            acknowledgment.acknowledge();

            log.info("환불 요청 처리 완료 - paymentId: {}, orderId: {}",
                    event.getPaymentId(), event.getOrderId());

        } catch (Exception e) {
            log.error("환불 요청 처리 실패 - paymentId: {}, orderId: {}, error: {}",
                    event.getPaymentId(), event.getOrderId(), e.getMessage(), e);

            // 재시도를 위해 ACK 하지 않음
            throw e;
        }
    }
}