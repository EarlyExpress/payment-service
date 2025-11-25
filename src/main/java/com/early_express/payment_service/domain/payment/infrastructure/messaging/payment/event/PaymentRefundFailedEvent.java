package com.early_express.payment_service.domain.payment.infrastructure.messaging.payment.event;

import com.early_express.payment_service.domain.payment.domain.messaging.PaymentRefundFailedEventData;
import com.early_express.payment_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 환불 실패 이벤트 (Kafka 전송용)
 * Topic: payment-refund-failed
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class PaymentRefundFailedEvent extends BaseEvent {

    private String paymentId;
    private String orderId;
    private BigDecimal requestedRefundAmount;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime failedAt;

    /**
     * 도메인 이벤트 데이터로부터 Kafka 이벤트 생성
     */
    public static PaymentRefundFailedEvent from(PaymentRefundFailedEventData data) {
        PaymentRefundFailedEvent event = PaymentRefundFailedEvent.builder()
                .paymentId(data.getPaymentId())
                .orderId(data.getOrderId())
                .requestedRefundAmount(data.getRequestedRefundAmount())
                .errorMessage(data.getErrorMessage())
                .failedAt(data.getFailedAt())
                .build();

        event.initBaseEvent("PAYMENT_REFUND_FAILED", "payment-service");
        return event;
    }
}