package com.early_express.payment_service.domain.payment.domain.messaging;

import com.early_express.payment_service.domain.payment.domain.model.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 환불 실패 이벤트 데이터
 * 도메인 계층의 이벤트 데이터 (불변)
 */
@Getter
@Builder
public class PaymentRefundFailedEventData {

    private final String paymentId;
    private final String orderId;
    private final BigDecimal requestedRefundAmount;
    private final String errorMessage;
    private final LocalDateTime failedAt;

    /**
     * Payment 엔티티로부터 이벤트 데이터 생성
     */
    public static PaymentRefundFailedEventData from(
            Payment payment,
            String errorMessage) {

        return PaymentRefundFailedEventData.builder()
                .paymentId(payment.getIdValue())
                .orderId(payment.getOrderId())
                .requestedRefundAmount(payment.getAmount())
                .errorMessage(errorMessage)
                .failedAt(LocalDateTime.now())
                .build();
    }
}
