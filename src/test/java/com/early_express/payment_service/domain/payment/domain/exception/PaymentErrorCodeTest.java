package com.early_express.payment_service.domain.payment.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentErrorCodeTest {

    @DisplayName("에러코드가 정상적으로 생성되고 필드가 잘 매핑되는지 테스트")
    @Test
    void errorCode_basic_creation_test() {
        // given
        PaymentErrorCode errorCode = PaymentErrorCode.PAYMENT_NOT_FOUND;

        // then
        assertThat(errorCode).isNotNull();
        assertThat(errorCode.getCode()).isEqualTo("PAYMENT_001");
        assertThat(errorCode.getMessage()).isEqualTo("결제 정보를 찾을 수 없습니다.");
        assertThat(errorCode.getStatus()).isEqualTo(404);
    }
}
