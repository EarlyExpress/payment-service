package com.early_express.payment_service.domain.payment.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PaymentAmountInfo 테스트")
class PaymentAmountInfoTest {

    @Test
    @DisplayName("결제 금액 생성이 정상적으로 이루어진다")
    void createAmount_success() {
        PaymentAmountInfo info = PaymentAmountInfo.of(BigDecimal.valueOf(10000));

        assertThat(info.getAmount()).isEqualTo(BigDecimal.valueOf(10000));
        assertThat(info.getRefundedAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(info.getCurrency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("환불 추가가 정상적으로 동작하며 새로운 VO를 반환한다")
    void refundAmount_success() {
        PaymentAmountInfo info = PaymentAmountInfo.of(BigDecimal.valueOf(10000));
        PaymentAmountInfo refunded = info.withRefund(BigDecimal.valueOf(3000));

        assertThat(refunded.getRefundedAmount()).isEqualTo(BigDecimal.valueOf(3000));
        assertThat(refunded.getRefundableAmount()).isEqualTo(BigDecimal.valueOf(7000));
        assertThat(refunded.isPartiallyRefunded()).isTrue();
        assertThat(refunded.isFullyRefunded()).isFalse();
    }

    @Test
    @DisplayName("환불 금액이 결제 금액을 초과하면 예외가 발생한다")
    void refundAmount_fail_when_exceed() {
        PaymentAmountInfo info = PaymentAmountInfo.of(BigDecimal.valueOf(10000));

        assertThatThrownBy(() -> info.withRefund(BigDecimal.valueOf(20000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("초과");
    }
}
