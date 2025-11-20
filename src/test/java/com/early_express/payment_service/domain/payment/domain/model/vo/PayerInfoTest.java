package com.early_express.payment_service.domain.payment.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PayerInfo 테스트")
class PayerInfoTest {

    @Test
    @DisplayName("PayerInfo 생성이 정상적으로 동작한다")
    void createPayerInfo_success() {
        PayerInfo info = PayerInfo.of("COMP-002", "김준형", "test@test.com", "01012345678");

        assertThat(info.getPayerCompanyId()).isEqualTo("COMP-002");
        assertThat(info.getPayerName()).isEqualTo("김준형");
        assertThat(info.getPayerEmail()).isEqualTo("test@test.com");
        assertThat(info.getPayerPhone()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("필수값(null/빈 값) 입력 시 예외가 발생한다")
    void createPayerInfo_fail_when_invalid() {
        assertThatThrownBy(() -> PayerInfo.of(null, "김준형", null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PayerInfo.of("COMP-002", " ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
