package com.early_express.payment_service.domain.payment.domain.exception;

/**
 * 결제 검증 관련 예외
 */
public class PaymentVerificationException extends PaymentException {

    public PaymentVerificationException(PaymentErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentVerificationException(PaymentErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public PaymentVerificationException(PaymentErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public PaymentVerificationException(PaymentErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}