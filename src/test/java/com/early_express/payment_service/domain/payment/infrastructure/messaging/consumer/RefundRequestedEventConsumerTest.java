package com.early_express.payment_service.domain.payment.infrastructure.messaging.consumer;

import com.early_express.payment_service.domain.payment.application.service.PaymentService;
import com.early_express.payment_service.domain.payment.domain.client.TossPaymentsClient;
import com.early_express.payment_service.domain.payment.domain.exception.PaymentException;
import com.early_express.payment_service.domain.payment.domain.messaging.PaymentEventPublisher;
import com.early_express.payment_service.domain.payment.domain.model.Payment;
import com.early_express.payment_service.domain.payment.domain.model.PaymentStatus;
import com.early_express.payment_service.domain.payment.domain.repository.PaymentRepository;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossCancelResponse;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossPaymentVerifyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * RefundRequestedEventConsumer 통합 테스트
 * Mock을 활용하여 환불 처리 로직 검증
 */
@SpringBootTest
@Transactional
@DisplayName("RefundRequestedEventConsumer 통합 테스트")
class RefundRequestedEventConsumerTest {

    @Autowired
    private RefundRequestedEventConsumer consumer;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private TossPaymentsClient tossPaymentsClient;

    @MockBean
    private PaymentEventPublisher eventPublisher;

    @MockBean
    private Acknowledgment acknowledgment;

    @Test
    @DisplayName("환불 요청 이벤트 수신 성공 - 결제가 취소되고 REFUNDED 상태가 된다")
    void handleRefundRequested_Success() {
        // given - 먼저 검증된 결제 생성
        String orderId = "ORDER-REFUND-001";
        String pgPaymentKey = "pg-key-refund-001";
        BigDecimal amount = new BigDecimal("10000");

        // 결제 검증 Mock
        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-refund-001")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        // 결제 생성
        Payment payment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-refund-001", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // 환불 응답 Mock
        TossCancelResponse.CancelDetail cancelDetail = new TossCancelResponse.CancelDetail(
                amount,
                "고객 요청 환불",
                LocalDateTime.now(),
                "refund-tx-001"
        );

        TossCancelResponse cancelResponse = TossCancelResponse.builder()
                .status("CANCELED")
                .cancels(List.of(cancelDetail))
                .build();

        given(tossPaymentsClient.cancelPayment(eq(pgPaymentKey), any()))
                .willReturn(cancelResponse);

        // 환불 요청 이벤트 생성
        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("eventType", "RefundRequestedEvent");
        refundEvent.put("eventId", "event-refund-001");
        refundEvent.put("paymentId", payment.getIdValue());
        refundEvent.put("orderId", orderId);
        refundEvent.put("refundReason", "고객 요청 환불");
        refundEvent.put("requestedAt", LocalDateTime.now().toString());
        refundEvent.put("source", "order-service");

        // when
        consumer.handleRefundRequested(refundEvent, 0, 0L, acknowledgment);

        // then
        Payment refundedPayment = paymentRepository.findById(payment.getId())
                .orElseThrow();

        assertThat(refundedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refundedPayment.isFullyRefunded()).isTrue();

        verify(acknowledgment).acknowledge();
        verify(eventPublisher).publishPaymentRefunded(any());
    }

    @Test
    @DisplayName("환불 요청 이벤트 수신 - 다른 이벤트 타입은 무시한다")
    void handleRefundRequested_IgnoreOtherEventType() {
        // given
        Map<String, Object> otherEvent = new HashMap<>();
        otherEvent.put("eventType", "OrderCreatedEvent");
        otherEvent.put("eventId", "event-other-001");
        otherEvent.put("orderId", "ORDER-OTHER-001");

        // when
        consumer.handleRefundRequested(otherEvent, 0, 0L, acknowledgment);

        // then
        verify(acknowledgment).acknowledge();
        verify(tossPaymentsClient, never()).cancelPayment(anyString(), any());
        verify(eventPublisher, never()).publishPaymentRefunded(any());
    }

    @Test
    @DisplayName("환불 요청 이벤트 수신 실패 - PG 취소 실패 시 REFUND_FAILED 상태가 된다")
    void handleRefundRequested_PgCancelFailed() {
        // given - 먼저 검증된 결제 생성
        String orderId = "ORDER-REFUND-FAIL-001";
        String pgPaymentKey = "pg-key-fail-001";
        BigDecimal amount = new BigDecimal("10000");

        // 결제 검증 Mock
        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-fail-001")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        // 결제 생성
        Payment payment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-fail-001", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // PG 취소 실패 Mock
        given(tossPaymentsClient.cancelPayment(eq(pgPaymentKey), any()))
                .willThrow(new RuntimeException("PG 통신 오류"));

        // 환불 요청 이벤트 생성
        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("eventType", "RefundRequestedEvent");
        refundEvent.put("eventId", "event-fail-001");
        refundEvent.put("paymentId", payment.getIdValue());
        refundEvent.put("orderId", orderId);
        refundEvent.put("refundReason", "고객 요청 환불");
        refundEvent.put("requestedAt", LocalDateTime.now().toString());

        // when & then
        assertThatThrownBy(() ->
                consumer.handleRefundRequested(refundEvent, 0, 0L, acknowledgment)
        ).isInstanceOf(RuntimeException.class);

        // DB에서 확인
        Payment failedPayment = paymentRepository.findById(payment.getId())
                .orElseThrow();

        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.REFUND_FAILED);

        verify(eventPublisher).publishPaymentRefundFailed(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("환불 요청 이벤트 수신 - 주문 ID 불일치 시 예외 발생")
    void handleRefundRequested_OrderIdMismatch() {
        // given - 먼저 검증된 결제 생성
        String orderId = "ORDER-MISMATCH-001";
        String pgPaymentKey = "pg-key-mismatch-001";
        BigDecimal amount = new BigDecimal("10000");

        // 결제 검증 Mock
        TossPaymentVerifyResponse verifyResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(pgPaymentKey)
                .orderId(orderId)
                .status("DONE")
                .totalAmount(amount)
                .approvedAt(LocalDateTime.now())
                .transactionKey("tx-mismatch-001")
                .build();

        given(tossPaymentsClient.getPayment(pgPaymentKey))
                .willReturn(verifyResponse);

        // 결제 생성
        Payment payment = paymentService.verifyAndRegisterPayment(
                orderId, "TOSS", "pg-payment-mismatch-001", pgPaymentKey, amount,
                "COMPANY-001", "홍길동", "test@example.com", "010-1234-5678",
                "COMPANY-002", "수취업체"
        );

        // 환불 요청 이벤트 생성 (잘못된 주문 ID)
        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("eventType", "RefundRequestedEvent");
        refundEvent.put("eventId", "event-mismatch-001");
        refundEvent.put("paymentId", payment.getIdValue());
        refundEvent.put("orderId", "WRONG-ORDER-ID");
        refundEvent.put("refundReason", "고객 요청 환불");
        refundEvent.put("requestedAt", LocalDateTime.now().toString());

        // when & then
        assertThatThrownBy(() ->
                consumer.handleRefundRequested(refundEvent, 0, 0L, acknowledgment)
        ).isInstanceOf(PaymentException.class);

        // 환불 처리되지 않음 (원래 상태 유지)
        Payment unchangedPayment = paymentRepository.findById(payment.getId())
                .orElseThrow();

        assertThat(unchangedPayment.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("환불 요청 이벤트 수신 - 존재하지 않는 결제 ID")
    void handleRefundRequested_PaymentNotFound() {
        // given
        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("eventType", "RefundRequestedEvent");
        refundEvent.put("eventId", "event-notfound-001");
        refundEvent.put("paymentId", "non-existent-payment-id");
        refundEvent.put("orderId", "ORDER-NOTFOUND-001");
        refundEvent.put("refundReason", "고객 요청 환불");
        refundEvent.put("requestedAt", LocalDateTime.now().toString());

        // when & then
        assertThatThrownBy(() ->
                consumer.handleRefundRequested(refundEvent, 0, 0L, acknowledgment)
        ).isInstanceOf(PaymentException.class);

        verify(eventPublisher, never()).publishPaymentRefunded(any());
        verify(eventPublisher, never()).publishPaymentRefundFailed(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("환불 요청 이벤트 수신 - 필수 필드 누락 시 예외 처리")
    void handleRefundRequested_MissingRequiredField() {
        // given - paymentId 누락
        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("eventType", "RefundRequestedEvent");
        refundEvent.put("eventId", "event-missing-001");
        // paymentId 누락
        refundEvent.put("orderId", "ORDER-MISSING-001");
        refundEvent.put("refundReason", "고객 요청 환불");

        // when & then
        assertThatThrownBy(() ->
                consumer.handleRefundRequested(refundEvent, 0, 0L, acknowledgment)
        ).isInstanceOf(Exception.class);

        verify(eventPublisher, never()).publishPaymentRefunded(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("환불 요청 이벤트 수신 - null 이벤트")
    void handleRefundRequested_NullEvent() {
        // when & then
        assertThatThrownBy(() ->
                consumer.handleRefundRequested(null, 0, 0L, acknowledgment)
        ).isInstanceOf(Exception.class);

        verify(acknowledgment, never()).acknowledge();
    }
}