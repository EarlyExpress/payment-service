package com.early_express.payment_service.domain.payment.infrastructure.messaging.order.event;

import com.early_express.payment_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 환불 요청 이벤트 (Order Service → Payment Service)
 * 수신 전용 이벤트
 * Topic: refund-requested
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class RefundRequestedEvent extends BaseEvent {

    private String paymentId;
    private String orderId;
    private String refundReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime requestedAt;
}