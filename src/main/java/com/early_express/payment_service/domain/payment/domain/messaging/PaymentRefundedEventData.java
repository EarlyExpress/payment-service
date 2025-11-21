package com.early_express.payment_service.domain.payment.domain.messaging;

import com.early_express.payment_service.domain.payment.domain.model.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 환불 완료 이벤트 데이터
 * 도메인 계층의 이벤트 데이터 (불변)
 */
@Getter
@Builder
public class PaymentRefundedEventData {

    private final String paymentId;
    private final String orderId;
    private final BigDecimal refundAmount;
    private final BigDecimal totalRefundedAmount;
    private final String refundReason;
    private final String pgRefundId;
    private final boolean fullRefund;
    private final LocalDateTime refundedAt;

    /**
     * Payment 엔티티로부터 이벤트 데이터 생성
     */
    public static PaymentRefundedEventData from(
            Payment payment,
            BigDecimal refundAmount,
            String refundReason) {

        return PaymentRefundedEventData.builder()
                .paymentId(payment.getIdValue())
                .orderId(payment.getOrderId())
                .refundAmount(refundAmount)
                .totalRefundedAmount(payment.getRefundedAmount())
                .refundReason(refundReason)
                .pgRefundId(payment.getPgInfo().getPgRefundId())
                .fullRefund(payment.isFullyRefunded())
                .refundedAt(payment.getRefundedAt())
                .build();
    }
}
