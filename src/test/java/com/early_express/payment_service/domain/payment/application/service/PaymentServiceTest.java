package com.early_express.payment_service.domain.payment.application.service;

import com.early_express.payment_service.domain.payment.domain.client.TossPaymentsClient;
import com.early_express.payment_service.domain.payment.domain.exception.PaymentException;
import com.early_express.payment_service.domain.payment.domain.exception.PaymentVerificationException;
import com.early_express.payment_service.domain.payment.domain.exception.RefundException;
import com.early_express.payment_service.domain.payment.domain.messaging.PaymentEventPublisher;
import com.early_express.payment_service.domain.payment.domain.model.Payment;
import com.early_express.payment_service.domain.payment.domain.model.PaymentStatus;
import com.early_express.payment_service.domain.payment.domain.repository.PaymentRepository;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossCancelRequest;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossCancelResponse;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossPaymentVerifyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * PaymentService 통합 테스트
 * - Repository는 실제 DB 사용
 * - TossPaymentsClient와 EventPublisher는 Mock
 */
@SpringBootTest
@Transactional
@DisplayName("PaymentService 통합 테스트")
class PaymentServiceTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private TossPaymentsClient tossPaymentsClient;

    @MockBean
    private PaymentEventPublisher eventPublisher;

    @Autowired
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 검증 성공 - Payment를 생성하고 VERIFIED 상태로 저장한다")
    void verifyAndRegisterPayment_Success() {
        // given
        String orderId = "ORDER-001";
        String pgPaymentKey = "pg-key-123";
        BigDecimal amount = new BigDecimal("10000");

        TossPaymentVerifyResponse tossResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-123")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(tossResponse);

        // when
        Payment result = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-123", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull(); // ID가 자동 생성됨
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getOrderId()).isEqualTo(orderId);

        // DB에 실제로 저장되었는지 확인
        Payment savedPayment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
    }

    @Test
    @DisplayName("결제 검증 실패 - 금액 불일치 시 VERIFICATION_FAILED 상태로 저장한다")
    void verifyAndRegisterPayment_AmountMismatch() {
        // given
        String orderId = "ORDER-002";
        String pgPaymentKey = "pg-key-456";
        BigDecimal expectedAmount = new BigDecimal("10000");
        BigDecimal actualAmount = new BigDecimal("20000");

        TossPaymentVerifyResponse tossResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(actualAmount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-123")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(tossResponse);

        // when & then
        assertThatThrownBy(() -> paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-456", pgPaymentKey, expectedAmount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        )).isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("일치하지 않습니다");

        // DB에 VERIFICATION_FAILED 상태로 저장되었는지 확인
        Payment savedPayment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("결제 검증 실패 - PG 결제 상태가 DONE이 아니면 예외 발생")
    void verifyAndRegisterPayment_NotDoneStatus() {
        // given
        TossPaymentVerifyResponse tossResponse = TossPaymentVerifyResponse.builder()
                .paymentKey("pg-key-789")
                .status("IN_PROGRESS")
                .totalAmount(new BigDecimal("10000"))
                .build();

        given(tossPaymentsClient.getPayment(anyString()))
                .willReturn(tossResponse);

        // when & then
        assertThatThrownBy(() -> paymentService.verifyAndRegisterPayment(
                "ORDER-003", "TOSS", "pg-payment-789", "pg-key-789", new BigDecimal("10000"),
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        )).isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("완료되지 않았습니다");
    }

    @Test
    @DisplayName("결제 검증 - 이미 처리된 결제는 기존 결제를 반환한다 (멱등성)")
    void verifyAndRegisterPayment_Idempotent() {
        // given - 첫 번째 결제 생성
        String orderId = "ORDER-004";
        String pgPaymentId = "pg-payment-999";
        String pgPaymentKey = "pg-key-999";
        BigDecimal amount = new BigDecimal("10000");

        TossPaymentVerifyResponse tossResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-123")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(tossResponse);

        // 첫 번째 호출
        Payment firstPayment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", pgPaymentId, pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // when - 두 번째 호출 (동일한 pgPaymentId)
        Payment secondPayment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", pgPaymentId, pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // then
        assertThat(secondPayment.getIdValue()).isEqualTo(firstPayment.getIdValue());
        verify(tossPaymentsClient, times(1)).getPayment(anyString()); // 한 번만 호출
    }

    @Test
    @DisplayName("결제 취소 성공 - 전액 환불 처리 후 REFUNDED 상태가 된다")
    void cancelPayment_Success() {
        // given - 먼저 검증된 결제 생성
        String orderId = "ORDER-005";
        String pgPaymentKey = "pg-key-111";
        BigDecimal amount = new BigDecimal("10000");

        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-111")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        Payment payment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-111", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // 취소 응답 Mock 설정
        TossCancelResponse.CancelDetail cancelDetail = new TossCancelResponse.CancelDetail(
                amount,
                "고객 요청",
                LocalDateTime.now(),
                "refund-tx-111"
        );

        TossCancelResponse cancelResponse = TossCancelResponse.builder()
                .status("CANCELED")
                .cancels(List.of(cancelDetail))
                .build();

        given(tossPaymentsClient.cancelPayment(eq(pgPaymentKey), any(TossCancelRequest.class)))
                .willReturn(cancelResponse);

        // when
        Payment result = paymentService.cancelPayment(payment.getIdValue(), orderId, "고객 요청");

        // then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.getRefundedAmount()).isEqualTo(amount);
        assertThat(result.isFullyRefunded()).isTrue();

        verify(eventPublisher).publishPaymentRefunded(any());
    }

    @Test
    @DisplayName("결제 취소 실패 - 주문 ID가 일치하지 않으면 예외 발생")
    void cancelPayment_OrderIdMismatch() {
        // given - 먼저 검증된 결제 생성
        String orderId = "ORDER-006";
        String pgPaymentKey = "pg-key-222";
        BigDecimal amount = new BigDecimal("10000");

        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-222")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        Payment payment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-222", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // when & then
        String wrongOrderId = "WRONG-ORDER";
        assertThatThrownBy(() ->
                paymentService.cancelPayment(payment.getIdValue(), wrongOrderId, "고객 요청")
        ).isInstanceOf(PaymentException.class)
                .hasMessageContaining("일치하지 않습니다");
    }

    @Test
    @DisplayName("결제 취소 실패 - PG 취소 요청 실패 시 REFUND_FAILED 상태가 된다")
    void cancelPayment_TossApiFailure() {
        // given - 먼저 검증된 결제 생성
        String orderId = "ORDER-007";
        String pgPaymentKey = "pg-key-333";
        BigDecimal amount = new BigDecimal("10000");

        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-333")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        Payment payment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-333", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // PG 취소 실패 Mock 설정
        given(tossPaymentsClient.cancelPayment(eq(pgPaymentKey), any()))
                .willThrow(new RuntimeException("PG 통신 오류"));

        // when & then
        assertThatThrownBy(() ->
                paymentService.cancelPayment(payment.getIdValue(), orderId, "고객 요청")
        ).isInstanceOf(RefundException.class);

        // DB에서 확인
        Payment failedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.REFUND_FAILED);

        verify(eventPublisher).publishPaymentRefundFailed(any());
    }

    @Test
    @DisplayName("결제 조회 성공 - ID로 결제를 조회한다")
    void findById_Success() {
        // given - 먼저 결제 생성
        String orderId = "ORDER-008";
        String pgPaymentKey = "pg-key-444";
        BigDecimal amount = new BigDecimal("10000");

        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-444")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        Payment payment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-444", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // when
        Payment result = paymentService.findById(payment.getIdValue());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIdValue()).isEqualTo(payment.getIdValue());
    }

    @Test
    @DisplayName("결제 조회 실패 - 존재하지 않는 ID로 조회 시 예외 발생")
    void findById_NotFound() {
        // given
        String nonExistentId = "non-existent-id";

        // when & then
        assertThatThrownBy(() -> paymentService.findById(nonExistentId))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    @Test
    @DisplayName("주문 ID로 결제 조회 성공")
    void findByOrderId_Success() {
        // given - 먼저 결제 생성
        String orderId = "ORDER-009";
        String pgPaymentKey = "pg-key-555";
        BigDecimal amount = new BigDecimal("10000");

        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-555")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-555", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // when
        Payment result = paymentService.findByOrderId(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("결제 검증 여부 확인")
    void isVerified() {
        // given - 검증된 결제 생성
        String orderId = "ORDER-010";
        String pgPaymentKey = "pg-key-666";
        BigDecimal amount = new BigDecimal("10000");

        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-666")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        Payment payment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-666", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // when
        boolean result = paymentService.isVerified(payment.getIdValue());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("환불 가능 여부 확인")
    void isRefundable() {
        // given - 검증된 결제 생성
        String orderId = "ORDER-011";
        String pgPaymentKey = "pg-key-777";
        BigDecimal amount = new BigDecimal("10000");

        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-777")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        Payment payment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-777", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // when
        boolean result = paymentService.isRefundable(payment.getIdValue());

        // then
        assertThat(result).isTrue();
    }
}