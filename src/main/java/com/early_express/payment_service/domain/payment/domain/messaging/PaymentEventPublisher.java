package com.early_express.payment_service.domain.payment.domain.messaging;

/**
 * Payment Event Publisher 인터페이스
 * 도메인 계층의 이벤트 발행 계약
 */
public interface PaymentEventPublisher {

    /**
     * 결제 환불 완료 이벤트 발행
     */
    void publishPaymentRefunded(PaymentRefundedEventData data);

    /**
     * 결제 환불 실패 이벤트 발행
     */
    void publishPaymentRefundFailed(PaymentRefundFailedEventData data);
}
