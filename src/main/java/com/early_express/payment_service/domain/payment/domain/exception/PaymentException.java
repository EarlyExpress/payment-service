package com.early_express.payment_service.domain.payment.domain.exception;

import com.early_express.payment_service.global.presentation.exception.GlobalException;

/**
 * Payment Service의 모든 예외의 기본 클래스
 */
public class PaymentException extends GlobalException {

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentException(PaymentErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public PaymentException(PaymentErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public PaymentException(PaymentErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}