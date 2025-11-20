package com.early_express.payment_service.domain.payment.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentExceptionTest {

    @Test
    @DisplayName("PaymentException이 정상적으로 생성되고 ErrorCode가 포함되는지 테스트")
    void exception_basic_creation_test() {
        // given
        PaymentErrorCode errorCode = PaymentErrorCode.INVALID_PAYMENT_STATUS;

        // when
        PaymentException exception = new PaymentException(errorCode);

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("PaymentException 생성 시 커스텀 메시지가 정상적으로 override 되는지 테스트")
    void exception_custom_message_test() {
        // given
        PaymentErrorCode errorCode = PaymentErrorCode.PG_CONNECTION_FAILED;
        String customMessage = "PG 서버 연결 불가";

        // when
        PaymentException exception = new PaymentException(errorCode, customMessage);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("PaymentException 생성 시 cause(원인 예외)가 정상적으로 전달되는지 테스트")
    void exception_with_cause_test() {
        // given
        PaymentErrorCode errorCode = PaymentErrorCode.PG_TIMEOUT;
        RuntimeException cause = new RuntimeException("Timeout 발생");

        // when
        PaymentException exception = new PaymentException(errorCode, cause);

        // then
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}