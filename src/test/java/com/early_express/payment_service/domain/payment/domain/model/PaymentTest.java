package com.early_express.payment_service.domain.payment.domain.model;

import com.early_express.payment_service.domain.payment.domain.exception.PaymentException;
import com.early_express.payment_service.domain.payment.domain.exception.PaymentVerificationException;
import com.early_express.payment_service.domain.payment.domain.exception.RefundException;
import com.early_express.payment_service.domain.payment.domain.model.vo.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Payment 도메인 테스트")
class PaymentTest {

    private PgInfo validPgInfo() {
        return PgInfo.of(
                "TOSS",
                "PG-PAY",
                "KEY",
                "TRANS",
                LocalDateTime.now().minusMinutes(5)
        );
    }

    private PayerInfo payer() {
        return PayerInfo.of("COMP-A", "홍길동", "a@test.com", "01012345678");
    }

    private PayeeInfo payee() {
        return PayeeInfo.of("COMP-B", "김수령");
    }

    private Payment payment() {
        return Payment.create(
                "ORDER-123",
                BigDecimal.valueOf(10000),
                validPgInfo(),
                payer(),
                payee()
        );
    }

    // ========================================================================
    // 1. 결제 생성
    // ========================================================================

    @Nested
    @DisplayName("결제 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("Payment.create()는 정상적으로 결제를 생성한다")
        void create_success() {
            Payment payment = payment();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getOrderId()).isEqualTo("ORDER-123");
            assertThat(payment.getAmount()).isEqualTo(BigDecimal.valueOf(10000));
        }
    }

    // ========================================================================
    // 2. 검증 로직
    // ========================================================================

    @Nested
    @DisplayName("결제 검증 테스트")
    class VerificationTest {

        @Test
        @DisplayName("startVerification() 호출 시 상태가 VERIFYING으로 변경된다")
        void startVerification_success() {
            Payment payment = payment();

            payment.startVerification();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VERIFYING);
        }

        @Test
        @DisplayName("PENDING 상태가 아니면 startVerification() 실패")
        void startVerification_fail() {
            Payment payment = payment();
            payment.startVerification();

            assertThatThrownBy(payment::startVerification)
                    .isInstanceOf(PaymentException.class);
        }

        @Test
        @DisplayName("verifySuccess() 호출 시 status = VERIFIED")
        void verifySuccess_success() {
            Payment payment = payment();
            payment.startVerification();

            payment.verifySuccess();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
            assertThat(payment.getVerificationStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("PG 승인시간 1시간 초과 시 verifySuccess() 실패")
        void verifySuccess_fail_expired() {
            PgInfo expired = PgInfo.of(
                    "TOSS",
                    "P1",
                    "KEY",
                    "T1",
                    LocalDateTime.now().minusHours(2)
            );

            Payment payment = Payment.create(
                    "O1",
                    BigDecimal.valueOf(10000),
                    expired,
                    payer(),
                    payee()
            );

            payment.startVerification();

            assertThatThrownBy(payment::verifySuccess)
                    .isInstanceOf(PaymentVerificationException.class);
        }
    }

    // ========================================================================
    // 3. 금액 검증
    // ========================================================================

    @Nested
    @DisplayName("결제 금액 검증 테스트")
    class AmountValidationTest {

        @Test
        @DisplayName("validateAmount(): 금액 일치 시 성공")
        void validateAmount_success() {
            Payment payment = payment();

            assertThatCode(() -> payment.validateAmount(BigDecimal.valueOf(10000)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateAmount(): 불일치 시 실패")
        void validateAmount_fail_mismatch() {
            Payment payment = payment();

            assertThatThrownBy(() -> payment.validateAmount(BigDecimal.valueOf(5000)))
                    .isInstanceOf(PaymentVerificationException.class);
        }
    }

    // ========================================================================
    // 4. 환불 관련 테스트
    // ========================================================================

    @Nested
    @DisplayName("환불 처리 테스트")
    class RefundTest {

        private Payment verifiedPayment() {
            Payment p = payment();
            p.startVerification();
            p.verifySuccess();
            return p;
        }

        @Test
        @DisplayName("startRefund(): 정상 시작 시 REFUNDING 상태로 변경")
        void startRefund_success() {
            Payment p = verifiedPayment();

            p.startRefund(BigDecimal.valueOf(5000), "사유");

            assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDING);
        }

        @Test
        @DisplayName("startRefund(): 환불 금액이 초과하면 실패")
        void startRefund_fail_exceed() {
            Payment p = verifiedPayment();

            assertThatThrownBy(
                    () -> p.startRefund(BigDecimal.valueOf(50000), "사유")
            ).isInstanceOf(RefundException.class);
        }

        @Test
        @DisplayName("completeRefund(): 부분 환불 성공")
        void completeRefund_partial() {
            Payment p = verifiedPayment();
            p.startRefund(BigDecimal.valueOf(3000), "사유");

            p.completeRefund(BigDecimal.valueOf(3000), "PG-R1");

            assertThat(p.isPartiallyRefunded()).isTrue();
            assertThat(p.getRefundedAmount()).isEqualTo(BigDecimal.valueOf(3000));
        }

        @Test
        @DisplayName("completeRefund(): 전액 환불 시 REFUNDED 상태 전환")
        void completeRefund_full() {
            Payment p = verifiedPayment();
            p.startRefund(BigDecimal.valueOf(10000), "전액");

            p.completeRefund(BigDecimal.valueOf(10000), "PG-R1");

            assertThat(p.isFullyRefunded()).isTrue();
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }
    }

    // ========================================================================
    // 5. 취소 테스트
    // ========================================================================

    @Nested
    @DisplayName("결제 취소 테스트")
    class CancelTest {

        @Test
        @DisplayName("cancel(): 정상적으로 취소됨")
        void cancel_success() {
            Payment payment = payment();

            payment.cancel("사유");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        @Test
        @DisplayName("cancel(): final 상태에서는 취소 불가")
        void cancel_fail_finalState() {
            Payment payment = payment();
            payment.cancel("사유"); // CANCELLED → final state

            assertThatThrownBy(() -> payment.cancel("다시 취소"))
                    .isInstanceOf(PaymentException.class);
        }
    }
}
