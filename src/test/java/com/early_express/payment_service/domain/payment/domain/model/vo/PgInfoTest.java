package com.early_express.payment_service.domain.payment.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PgInfo 테스트")
class PgInfoTest {

    @Test
    @DisplayName("PgInfo.of()가 정상적으로 PG 정보를 생성한다")
    void createPgInfo_success() {
        LocalDateTime approvedAt = LocalDateTime.now().minusMinutes(10);

        PgInfo info = PgInfo.of(
                "TOSS",
                "PG-PAY-ID",
                "KEY-123",
                "TRANS-123",
                approvedAt
        );

        assertThat(info.getPgProvider()).isEqualTo("TOSS");
        assertThat(info.getPgPaymentId()).isEqualTo("PG-PAY-ID");
        assertThat(info.getPgApprovedAt()).isEqualTo(approvedAt);
    }

    @Test
    @DisplayName("PG 환불 정보가 정상적으로 추가된다")
    void addRefundInfo_success() {
        PgInfo info = PgInfo.of(
                "TOSS",
                "PG-PAY-ID",
                "KEY",
                "TRANS",
                LocalDateTime.now()
        );

        LocalDateTime refundedAt = LocalDateTime.now();

        PgInfo refunded = info.withRefund("REFUND-001", refundedAt);

        assertThat(refunded.isRefunded()).isTrue();
        assertThat(refunded.getPgRefundId()).isEqualTo("REFUND-001");
    }

    @Test
    @DisplayName("PG 승인 시간이 1시간 이내이면 승인 유효 판단이 true")
    void approvalTimeValid_true() {
        PgInfo info = PgInfo.of(
                "PORTONE",
                "PG-PAY",
                "KEY",
                "TRANS",
                LocalDateTime.now().minusMinutes(30)
        );

        assertThat(info.isApprovalTimeValid()).isTrue();
    }

    @DisplayName("PG 승인 시간이 1시간 초과이면 승인 유효 판단이 false")
    @Test
    void approvalTimeValid_false() {
        PgInfo info = PgInfo.of(
                "PORTONE",
                "PG-PAY",
                "KEY",
                "TRANS",
                LocalDateTime.now().minusHours(2)
        );

        assertThat(info.isApprovalTimeValid()).isFalse();
    }
}
