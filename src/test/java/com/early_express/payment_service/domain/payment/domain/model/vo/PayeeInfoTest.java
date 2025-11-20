package com.early_express.payment_service.domain.payment.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PayeeInfo Value Object 테스트")
class PayeeInfoTest {

    @Test
    @DisplayName("PayeeInfo 생성이 정상적으로 동작한다")
    void createPayeeInfo_success() {
        PayeeInfo info = PayeeInfo.of("COMP-001", "홍길동");

        assertThat(info.getPayeeCompanyId()).isEqualTo("COMP-001");
        assertThat(info.getPayeeName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("PayeeInfo 생성 시 null 또는 빈 값이면 예외가 발생한다")
    void createPayeeInfo_fail_when_null() {
        assertThatThrownBy(() -> PayeeInfo.of(null, "홍길동"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PayeeInfo.of("COMP-001", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
