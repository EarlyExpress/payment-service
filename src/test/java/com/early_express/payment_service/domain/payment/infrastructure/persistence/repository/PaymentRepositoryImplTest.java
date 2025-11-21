package com.early_express.payment_service.domain.payment.infrastructure.persistence.repository;

import com.early_express.payment_service.domain.payment.domain.model.Payment;
import com.early_express.payment_service.domain.payment.domain.model.PaymentStatus;
import com.early_express.payment_service.domain.payment.domain.model.vo.*;
import com.early_express.payment_service.domain.payment.infrastructure.persistence.entity.PaymentEntity;
import com.early_express.payment_service.domain.payment.infrastructure.persistence.jpa.PaymentJpaRepository;
import com.early_express.payment_service.global.config.JpaConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * PaymentRepositoryImpl 통합 테스트
 */
@DataJpaTest
@Import({PaymentRepositoryImpl.class, JpaConfig.class})
@DisplayName("PaymentRepository 통합 테스트")
class PaymentRepositoryImplTest {

    @Autowired
    private PaymentRepositoryImpl paymentRepository;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    private Payment testPayment;
    private String testCompanyId = "COMPANY-001";
    private String testOrderId = "ORDER-001";

    @BeforeEach
    void setUp() {
        paymentJpaRepository.deleteAll();

        testPayment = createTestPayment(
                testOrderId,
                new BigDecimal("10000"),
                testCompanyId,
                "COMPANY-002"
        );
    }

    @Nested
    @DisplayName("저장 테스트")
    class SaveTest {

        @Test
        @DisplayName("새로운 결제를 저장한다")
        void saveNewPayment() {
            // when
            Payment savedPayment = paymentRepository.save(testPayment);

            // then
            assertThat(savedPayment).isNotNull();
            assertThat(savedPayment.getId()).isNotNull();
            assertThat(savedPayment.getOrderId()).isEqualTo(testOrderId);
            assertThat(savedPayment.getAmount()).isEqualTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("기존 결제를 업데이트한다")
        void updateExistingPayment() {
            // given
            Payment savedPayment = paymentRepository.save(testPayment);
            savedPayment.startVerification();
            savedPayment.verifySuccess();

            // when
            Payment updatedPayment = paymentRepository.save(savedPayment);

            // then
            assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
            assertThat(updatedPayment.getVerifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 ID로 업데이트 시 예외가 발생한다")
        void updateNonExistentPayment() {
            // given
            Payment paymentWithInvalidId = Payment.builder()
                    .id(PaymentId.create())
                    .orderId("ORDER-999")
                    .amountInfo(PaymentAmountInfo.of(new BigDecimal("10000")))
                    .pgInfo(createTestPgInfo())
                    .payerInfo(createTestPayerInfo(testCompanyId))
                    .payeeInfo(createTestPayeeInfo("COMPANY-002"))
                    .status(PaymentStatus.PENDING)
                    .verificationStatus("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            // when & then
            assertThatThrownBy(() -> paymentRepository.save(paymentWithInvalidId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Payment not found");
        }
    }

    @Nested
    @DisplayName("조회 테스트")
    class FindTest {

        private Payment savedPayment;

        @BeforeEach
        void setUp() {
            savedPayment = paymentRepository.save(testPayment);
        }

        @Test
        @DisplayName("ID로 결제를 조회한다")
        void findById() {
            // when
            Optional<Payment> found = paymentRepository.findById(savedPayment.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getIdValue()).isEqualTo(savedPayment.getIdValue());
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional을 반환한다")
        void findByIdNotFound() {
            // when
            Optional<Payment> found = paymentRepository.findById(PaymentId.create());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("삭제된 결제는 조회되지 않는다")
        void findByIdExcludesDeleted() {
            // given
            paymentRepository.delete(savedPayment, "TEST_USER");

            // when
            Optional<Payment> found = paymentRepository.findById(savedPayment.getId());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("OrderId로 결제를 조회한다")
        void findByOrderId() {
            // when
            Optional<Payment> found = paymentRepository.findByOrderId(testOrderId);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getOrderId()).isEqualTo(testOrderId);
        }

        @Test
        @DisplayName("PG 결제 ID로 결제를 조회한다")
        void findByPgPaymentId() {
            // when
            Optional<Payment> found = paymentRepository.findByPgPaymentId("pg-payment-123");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getPgInfo().getPgPaymentId()).isEqualTo("pg-payment-123");
        }

        @Test
        @DisplayName("PG 결제 ID 존재 여부를 확인한다")
        void existsByPgPaymentId() {
            // when
            boolean exists = paymentRepository.existsByPgPaymentId("pg-payment-123");
            boolean notExists = paymentRepository.existsByPgPaymentId("non-existent-id");

            // then
            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }

        @Test
        @DisplayName("결제자 업체 ID로 결제 목록을 조회한다")
        void findByPayerCompanyId() {
            // given
            Payment payment2 = createTestPayment("ORDER-002", new BigDecimal("20000"), testCompanyId, "COMPANY-003");
            paymentRepository.save(payment2);

            // when
            List<Payment> payments = paymentRepository.findByPayerCompanyId(testCompanyId);

            // then
            assertThat(payments).hasSize(2);
            assertThat(payments).extracting(Payment::getOrderId)
                    .containsExactly("ORDER-002", "ORDER-001"); // 최신순 정렬
        }

        @Test
        @DisplayName("상태로 결제 목록을 조회한다")
        void findByStatus() {
            // given
            savedPayment.startVerification();
            savedPayment.verifySuccess();
            paymentRepository.save(savedPayment);

            Payment payment2 = createTestPayment("ORDER-002", new BigDecimal("20000"), "COMPANY-003", "COMPANY-004");
            paymentRepository.save(payment2);

            // when
            List<Payment> verifiedPayments = paymentRepository.findByStatus(PaymentStatus.VERIFIED);
            List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);

            // then
            assertThat(verifiedPayments).hasSize(1);
            assertThat(verifiedPayments.get(0).getStatus()).isEqualTo(PaymentStatus.VERIFIED);
            assertThat(pendingPayments).hasSize(1);
            assertThat(pendingPayments.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("삭제 테스트")
    class DeleteTest {

        private Payment savedPayment;

        @BeforeEach
        void setUp() {
            savedPayment = paymentRepository.save(testPayment);
        }

        @Test
        @DisplayName("결제를 논리 삭제한다")
        void deletePayment() {
            // when
            paymentRepository.delete(savedPayment, "TEST_USER");

            // then
            Optional<Payment> found = paymentRepository.findById(savedPayment.getId());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("관리자용 조회에서는 삭제된 결제도 조회된다")
        void findByIdIncludingDeleted() {
            // given
            paymentRepository.delete(savedPayment, "TEST_USER");

            // when
            Optional<Payment> found = paymentRepository.findByIdIncludingDeleted(savedPayment.getId());

            // then
            assertThat(found).isPresent();
        }
    }

    @Nested
    @DisplayName("동적 쿼리 검색 테스트")
    class SearchTest {

        @BeforeEach
        void setUp() {
            // 다양한 테스트 데이터 생성
            Payment payment1 = createTestPayment("ORDER-001", new BigDecimal("10000"), "COMPANY-001", "COMPANY-002");
            payment1.startVerification();
            payment1.verifySuccess();
            paymentRepository.save(payment1);

            Payment payment2 = createTestPayment("ORDER-002", new BigDecimal("20000"), "COMPANY-001", "COMPANY-002");
            paymentRepository.save(payment2);

            Payment payment3 = createTestPayment("ORDER-003", new BigDecimal("30000"), "COMPANY-003", "COMPANY-004");
            payment3.startVerification();
            payment3.verifySuccess();
            paymentRepository.save(payment3);
        }

        @Test
        @DisplayName("업체 ID로 결제를 검색한다")
        void searchByCompanyId() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Payment> result = paymentRepository.searchPayments(
                    "COMPANY-001", null, null, null, null, null, null, pageable
            );

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("상태로 결제를 검색한다")
        void searchByStatus() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Payment> result = paymentRepository.searchPayments(
                    null, PaymentStatus.VERIFIED, null, null, null, null, null, pageable
            );

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(p -> p.getStatus() == PaymentStatus.VERIFIED);
        }

        @Test
        @DisplayName("PG사로 결제를 검색한다")
        void searchByPgProvider() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Payment> result = paymentRepository.searchPayments(
                    null, null, "TOSS", null, null, null, null, pageable
            );

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent()).allMatch(p -> p.getPgInfo().getPgProvider().equals("TOSS"));
        }

        @Test
        @DisplayName("금액 범위로 결제를 검색한다")
        void searchByAmountRange() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            BigDecimal minAmount = new BigDecimal("15000");
            BigDecimal maxAmount = new BigDecimal("35000");

            // when
            Page<Payment> result = paymentRepository.searchPayments(
                    null, null, null, minAmount, maxAmount, null, null, pageable
            );

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(p ->
                    p.getAmount().compareTo(minAmount) >= 0 &&
                            p.getAmount().compareTo(maxAmount) <= 0
            );
        }

        @Test
        @DisplayName("날짜 범위로 결제를 검색한다")
        void searchByDateRange() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            // when
            Page<Payment> result = paymentRepository.searchPayments(
                    null, null, null, null, null, startDate, endDate, pageable
            );

            // then
            assertThat(result.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("여러 조건을 조합하여 검색한다")
        void searchByMultipleConditions() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Payment> result = paymentRepository.searchPayments(
                    "COMPANY-001",
                    PaymentStatus.VERIFIED,
                    "TOSS",
                    new BigDecimal("5000"),
                    new BigDecimal("15000"),
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(1),
                    pageable
            );

            // then
            assertThat(result.getContent()).hasSize(1);
            Payment found = result.getContent().get(0);
            assertThat(found.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
            assertThat(found.getAmount()).isEqualTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("페이징이 정상 작동한다")
        void searchWithPaging() {
            // given
            Pageable pageable = PageRequest.of(0, 2);

            // when
            Page<Payment> result = paymentRepository.searchPayments(
                    null, null, null, null, null, null, null, pageable
            );

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("특수 조회 테스트")
    class SpecialFindTest {

        @BeforeEach
        void setUp() {
            // 검증 실패 결제
            Payment failedPayment = createTestPayment("ORDER-FAILED", new BigDecimal("10000"), "COMPANY-001", "COMPANY-002");
            failedPayment.startVerification();
            failedPayment.verifyFailed("금액 불일치");
            paymentRepository.save(failedPayment);

            // 검증 성공 결제
            Payment verifiedPayment = createTestPayment("ORDER-VERIFIED", new BigDecimal("20000"), "COMPANY-001", "COMPANY-002");
            verifiedPayment.startVerification();
            verifiedPayment.verifySuccess();
            paymentRepository.save(verifiedPayment);

            // 부분 환불 결제
            Payment partialRefundPayment = createTestPayment("ORDER-PARTIAL", new BigDecimal("30000"), "COMPANY-001", "COMPANY-002");
            partialRefundPayment.startVerification();
            partialRefundPayment.verifySuccess();
            partialRefundPayment.startRefund(new BigDecimal("10000"), "부분 취소");
            partialRefundPayment.completeRefund(new BigDecimal("10000"), "refund-123");
            paymentRepository.save(partialRefundPayment);
        }

        @Test
        @DisplayName("검증 실패한 결제 목록을 조회한다")
        void findVerificationFailedPayments() {
            // when
            List<Payment> result = paymentRepository.findVerificationFailedPayments();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.VERIFICATION_FAILED);
        }

        @Test
        @DisplayName("환불 가능한 결제 목록을 조회한다")
        void findRefundablePayments() {
            // when
            List<Payment> result = paymentRepository.findRefundablePayments("COMPANY-001");

            // then
            assertThat(result).hasSize(2); // VERIFIED, PARTIALLY_REFUNDED
            assertThat(result).allMatch(Payment::isRefundable);
        }

        @Test
        @DisplayName("삭제된 결제를 포함하여 전체 검색한다")
        void searchAllPaymentsIncludingDeleted() {
            // given
            Payment payment = createTestPayment("ORDER-TO-DELETE", new BigDecimal("10000"), "COMPANY-001", "COMPANY-002");
            Payment saved = paymentRepository.save(payment);
            paymentRepository.delete(saved, "TEST_USER");

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Payment> result = paymentRepository.searchAllPaymentsIncludingDeleted(
                    "COMPANY-001", null, null, null, null, pageable
            );

            // then
            assertThat(result.getContent()).hasSizeGreaterThan(3); // 삭제된 것도 포함
        }

        @Test
        @DisplayName("삭제 여부로 필터링하여 검색한다")
        void searchByIsDeleted() {
            // given
            Payment payment = createTestPayment("ORDER-TO-DELETE", new BigDecimal("10000"), "COMPANY-001", "COMPANY-002");
            Payment saved = paymentRepository.save(payment);
            paymentRepository.delete(saved, "TEST_USER");

            Pageable pageable = PageRequest.of(0, 10);

            // when - 삭제된 것만 조회
            Page<Payment> deletedResult = paymentRepository.searchAllPaymentsIncludingDeleted(
                    null, null, true, null, null, pageable
            );

            // when - 삭제되지 않은 것만 조회
            Page<Payment> activeResult = paymentRepository.searchAllPaymentsIncludingDeleted(
                    null, null, false, null, null, pageable
            );

            // then
            assertThat(deletedResult.getContent()).hasSizeGreaterThan(0);
            assertThat(activeResult.getContent()).hasSize(3);
        }
    }

    // ===== 테스트 헬퍼 메서드 =====

    private Payment createTestPayment(String orderId, BigDecimal amount, String payerCompanyId, String payeeCompanyId) {
        return Payment.create(
                orderId,
                amount,
                createTestPgInfo(),
                createTestPayerInfo(payerCompanyId),
                createTestPayeeInfo(payeeCompanyId)
        );
    }

    private PgInfo createTestPgInfo() {
        return PgInfo.of(
                "TOSS",
                "pg-payment-123",
                "pg-key-123",
                "transaction-123",
                LocalDateTime.now()
        );
    }

    private PayerInfo createTestPayerInfo(String companyId) {
        return PayerInfo.of(
                companyId,
                "홍길동",
                "test@example.com",
                "010-1234-5678"
        );
    }

    private PayeeInfo createTestPayeeInfo(String companyId) {
        return PayeeInfo.of(
                companyId,
                "수취업체"
        );
    }
}