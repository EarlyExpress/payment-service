package com.early_express.payment_service.domain.payment.infrastructure.client.toss;

import com.early_express.payment_service.domain.payment.domain.exception.PaymentErrorCode;
import com.early_express.payment_service.domain.payment.domain.exception.PaymentVerificationException;
import com.early_express.payment_service.domain.payment.domain.exception.RefundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Toss Payments Client 에러 디코더
 * Toss Payments API의 HTTP 에러를 도메인 예외로 변환
 */
@Slf4j
public class TossPaymentsErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Toss Payments API 호출 실패 - Method: {}, Status: {}",
                methodKey, response.status());

        // GET /v1/payments/{paymentKey} - 결제 조회 실패
        if (methodKey.contains("getPayment")) {
            return handleGetPaymentError(response);
        }

        // POST /v1/payments/{paymentKey}/cancel - 결제 취소 실패
        if (methodKey.contains("cancelPayment")) {
            return handleCancelPaymentError(response);
        }

        return defaultErrorDecoder.decode(methodKey, response);
    }

    private Exception handleGetPaymentError(Response response) {
        return switch (response.status()) {
            case 400 -> new PaymentVerificationException(
                    PaymentErrorCode.INVALID_PAYMENT_KEY,
                    "올바르지 않은 결제 키입니다."
            );
            case 401 -> new PaymentVerificationException(
                    PaymentErrorCode.PG_AUTH_FAILED,
                    "토스페이먼츠 인증에 실패했습니다."
            );
            case 404 -> new PaymentVerificationException(
                    PaymentErrorCode.PAYMENT_NOT_FOUND,
                    "결제 정보를 찾을 수 없습니다."
            );
            case 500 -> new PaymentVerificationException(
                    PaymentErrorCode.PG_SYSTEM_ERROR,
                    "토스페이먼츠 시스템 오류가 발생했습니다."
            );
            default -> new PaymentVerificationException(
                    PaymentErrorCode.PG_VERIFICATION_FAILED,
                    "토스페이먼츠 결제 검증에 실패했습니다."
            );
        };
    }

    private Exception handleCancelPaymentError(Response response) {
        return switch (response.status()) {
            case 400 -> new RefundException(
                    PaymentErrorCode.INVALID_CANCEL_REQUEST,
                    "올바르지 않은 취소 요청입니다."
            );
            case 401 -> new RefundException(
                    PaymentErrorCode.PG_AUTH_FAILED,
                    "토스페이먼츠 인증에 실패했습니다."
            );
            case 404 -> new RefundException(
                    PaymentErrorCode.PAYMENT_NOT_FOUND,
                    "결제 정보를 찾을 수 없습니다."
            );
            case 409 -> new RefundException(
                    PaymentErrorCode.REFUND_NOT_ALLOWED,
                    "취소할 수 없는 결제 상태입니다."
            );
            case 500 -> new RefundException(
                    PaymentErrorCode.PG_SYSTEM_ERROR,
                    "토스페이먼츠 시스템 오류가 발생했습니다."
            );
            default -> new RefundException(
                    PaymentErrorCode.REFUND_FAILED,
                    "토스페이먼츠 결제 취소에 실패했습니다."
            );
        };
    }
}
