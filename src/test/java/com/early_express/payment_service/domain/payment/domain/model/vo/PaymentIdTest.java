package com.early_express.payment_service.domain.payment.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PaymentId 테스트")
class PaymentIdTest {


    @Test
    @DisplayName("PaymentId.create()는 UUID 기반 ID를 생성한다")
    void createPaymentId_success() {
        PaymentId paymentId = PaymentId.create();

        assertThat(paymentId).isNotNull();
        assertThat(paymentId.getValue()).isNotBlank();
    }


    @Test
    @DisplayName("PaymentId.from()은 전달한 ID를 가진 PaymentId를 생성한다")
    void createPaymentId_fromString_success() {
        PaymentId paymentId = PaymentId.from("TEST-ID-123");

        assertThat(paymentId.getValue()).isEqualTo("TEST-ID-123");
        assertThat(paymentId.toString()).isEqualTo("TEST-ID-123");
    }

    @DisplayName("null 또는 빈 값 입력 시 예외 발생")
    @Test
    void createPaymentId_fail_when_invalid() {
        assertThatThrownBy(() -> PaymentId.from(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PaymentId.from(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
