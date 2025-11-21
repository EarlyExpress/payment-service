package com.early_express.payment_service.domain.payment.domain.exception;

/**
 * PG사 연동 관련 예외
 */
public class PgException extends PaymentException {

    public PgException(PaymentErrorCode errorCode) {
        super(errorCode);
    }

    public PgException(PaymentErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public PgException(PaymentErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public PgException(PaymentErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}