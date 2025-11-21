package com.early_express.payment_service.domain.payment.infrastructure.messaging.event;

import com.early_express.payment_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 환불 요청 이벤트 (Order Service → Payment Service)
 * Topic: order-service-events
 */
@Getter
@NoArgsConstructor
public class RefundRequestedEvent extends BaseEvent {

    private String paymentId;
    private String orderId;
    private String refundReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime requestedAt;

    @Builder
    private RefundRequestedEvent(
            String paymentId,
            String orderId,
            String refundReason,
            LocalDateTime requestedAt) {

        super.initBaseEvent("RefundRequestedEvent", "order-service");

        this.paymentId = paymentId;
        this.orderId = orderId;
        this.refundReason = refundReason;
        this.requestedAt = requestedAt != null ? requestedAt : LocalDateTime.now();
    }

    /**
     * 환불 요청 이벤트 생성
     */
    public static RefundRequestedEvent of(
            String paymentId,
            String orderId,
            String refundReason) {

        return RefundRequestedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .refundReason(refundReason)
                .requestedAt(LocalDateTime.now())
                .build();
    }
}