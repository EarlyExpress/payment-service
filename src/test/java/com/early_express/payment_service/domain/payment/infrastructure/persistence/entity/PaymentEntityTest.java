package com.early_express.payment_service.domain.payment.infrastructure.persistence.entity;

import com.early_express.payment_service.domain.payment.domain.model.Payment;
import com.early_express.payment_service.domain.payment.domain.model.PaymentStatus;
import com.early_express.payment_service.domain.payment.domain.model.vo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PaymentEntity 매핑 테스트")
class PaymentEntityTest {

    // ------------------------------------------------------------------------
    // 헬퍼 메서드
    // ------------------------------------------------------------------------

    private PgInfo pg() {
        return PgInfo.of(
                "TOSS",
                "PG123",
                "KEY123",
                "TRANS123",
                LocalDateTime.now().minusMinutes(10)
        );
    }

    private PayerInfo payer() {
        return PayerInfo.of("COMP-A", "홍길동", "test@test.com", "01012345678");
    }

    private PayeeInfo payee() {
        return PayeeInfo.of("COMP-B", "김수령");
    }

    private Payment payment() {
        return Payment.create(
                "ORDER-1",
                BigDecimal.valueOf(10000),
                pg(),
                payer(),
                payee()
        );
    }

    // ========================================================================
    // 1. fromDomain() 테스트
    // ========================================================================

    @Nested
    @DisplayName("fromDomain() 테스트")
    class FromDomainTest {

        @Test
        @DisplayName("도메인 객체로부터 PaymentEntity가 정상 생성된다")
        void fromDomain_success() {
            // given
            Payment domain = payment();

            // when
            PaymentEntity entity = PaymentEntity.fromDomain(domain);

            // then
            assertThat(entity.getId()).isEqualTo(domain.getIdValue());
            assertThat(entity.getOrderId()).isEqualTo("ORDER-1");
            assertThat(entity.getAmount()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(entity.getPayerName()).isEqualTo("홍길동");
            assertThat(entity.getPayeeCompanyId()).isEqualTo("COMP-B");
            assertThat(entity.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }
    }

    // ========================================================================
    // 2. toDomain() 테스트
    // ========================================================================

    @Nested
    @DisplayName("toDomain() 테스트")
    class ToDomainTest {

        @Test
        @DisplayName("PaymentEntity에서 Payment 도메인으로 정상 변환된다")
        void toDomain_success() {
            // given - 도메인 → 엔티티 변환
            Payment domain = payment();
            PaymentEntity entity = PaymentEntity.fromDomain(domain);

            // when - 엔티티 → 다시 도메인 변환
            Payment converted = entity.toDomain();

            // then - 모든 필드 동일성 검증
            assertThat(converted.getIdValue()).isEqualTo(domain.getIdValue());
            assertThat(converted.getOrderId()).isEqualTo(domain.getOrderId());

            // 금액 정보
            assertThat(converted.getAmount()).isEqualTo(domain.getAmount());
            assertThat(converted.getRefundedAmount()).isEqualTo(domain.getRefundedAmount());
            assertThat(converted.getAmountInfo().getCurrency())
                    .isEqualTo(domain.getAmountInfo().getCurrency());

            // PG 정보
            assertThat(converted.getPgInfo().getPgProvider())
                    .isEqualTo(domain.getPgInfo().getPgProvider());
            assertThat(converted.getPgInfo().getPgPaymentId())
                    .isEqualTo(domain.getPgInfo().getPgPaymentId());

            // 결제자 / 수취인
            assertThat(converted.getPayerInfo().getPayerName())
                    .isEqualTo(domain.getPayerInfo().getPayerName());
            assertThat(converted.getPayeeInfo().getPayeeName())
                    .isEqualTo(domain.getPayeeInfo().getPayeeName());

            assertThat(converted.getStatus()).isEqualTo(domain.getStatus());
        }
    }

    // ========================================================================
    // 3. updateFromDomain() 테스트
    // ========================================================================

    @Nested
    @DisplayName("updateFromDomain() 테스트")
    class UpdateFromDomainTest {

        @Test
        @DisplayName("도메인 변경사항이 엔티티에 정확히 반영된다")
        void updateFromDomain_success() {
            // given
            Payment domain = payment();
            PaymentEntity entity = PaymentEntity.fromDomain(domain);

            // 상태 변경 시나리오
            domain.startVerification();
            domain.verifySuccess();
            domain.startRefund(BigDecimal.valueOf(3000), "고객 요청");
            domain.completeRefund(BigDecimal.valueOf(3000), "PG-REFUND-1");

            // when
            entity.updateFromDomain(domain);

            // then
            assertThat(entity.getStatus()).isEqualTo(domain.getStatus());
            assertThat(entity.getRefundedAmount()).isEqualTo(BigDecimal.valueOf(3000));
            assertThat(entity.getPgRefundId()).isEqualTo("PG-REFUND-1");
            assertThat(entity.getPgRefundedAt()).isNotNull();
            assertThat(entity.getRefundReason()).isEqualTo(domain.getRefundReason());
        }
    }
}
