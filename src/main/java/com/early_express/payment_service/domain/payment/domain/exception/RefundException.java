package com.early_express.payment_service.domain.payment.domain.exception;

/**
 * 환불 관련 예외
 */
public class RefundException extends PaymentException {

    public RefundException(PaymentErrorCode errorCode) {
        super(errorCode);
    }

    public RefundException(PaymentErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RefundException(PaymentErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public RefundException(PaymentErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}