package com.early_express.payment_service.domain.payment.infrastructure.messaging.event;

import com.early_express.payment_service.domain.payment.domain.messaging.PaymentRefundedEventData;
import com.early_express.payment_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 환불 완료 이벤트 (Kafka 전송용)
 * Topic: payment-events
 */
@Getter
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

    @Builder
    private PaymentRefundedEvent(
            String paymentId,
            String orderId,
            BigDecimal refundAmount,
            BigDecimal totalRefundedAmount,
            String refundReason,
            String pgRefundId,
            boolean fullRefund,
            LocalDateTime refundedAt) {

        super.initBaseEvent("PaymentRefundedEvent", "payment-service");

        this.paymentId = paymentId;
        this.orderId = orderId;
        this.refundAmount = refundAmount;
        this.totalRefundedAmount = totalRefundedAmount;
        this.refundReason = refundReason;
        this.pgRefundId = pgRefundId;
        this.fullRefund = fullRefund;
        this.refundedAt = refundedAt;
    }

    /**
     * 도메인 이벤트 데이터로부터 Kafka 이벤트 생성
     */
    public static PaymentRefundedEvent from(PaymentRefundedEventData data) {
        return PaymentRefundedEvent.builder()
                .paymentId(data.getPaymentId())
                .orderId(data.getOrderId())
                .refundAmount(data.getRefundAmount())
                .totalRefundedAmount(data.getTotalRefundedAmount())
                .refundReason(data.getRefundReason())
                .pgRefundId(data.getPgRefundId())
                .fullRefund(data.isFullRefund())
                .refundedAt(data.getRefundedAt())
                .build();
    }
}
