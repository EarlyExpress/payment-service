package com.early_express.payment_service.domain.payment.infrastructure.messaging.payment.event;

import com.early_express.payment_service.domain.payment.domain.messaging.PaymentRefundedEventData;
import com.early_express.payment_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 환불 완료 이벤트 (Kafka 전송용)
 * Topic: payment-refunded
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class PaymentRefundedEvent extends BaseEvent {

    private String paymentId;
    private String orderId;
    private BigDecimal refundAmount;
    private BigDecimal totalRefundedAmount;
    private String refundReason;
    private String pgRefundId;
    private boolean fullRefund;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime refundedAt;

    /**
     * 도메인 이벤트 데이터로부터 Kafka 이벤트 생성
     */
    public static PaymentRefundedEvent from(PaymentRefundedEventData data) {
        PaymentRefundedEvent event = PaymentRefundedEvent.builder()
                .paymentId(data.getPaymentId())
                .orderId(data.getOrderId())
                .refundAmount(data.getRefundAmount())
                .totalRefundedAmount(data.getTotalRefundedAmount())
                .refundReason(data.getRefundReason())
                .pgRefundId(data.getPgRefundId())
                .fullRefund(data.isFullRefund())
                .refundedAt(data.getRefundedAt())
                .build();

        event.initBaseEvent("PAYMENT_REFUNDED", "payment-service");
        return event;
    }
}