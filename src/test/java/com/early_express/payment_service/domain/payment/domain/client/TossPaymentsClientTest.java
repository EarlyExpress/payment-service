package com.early_express.payment_service.domain.payment.domain.client;

import com.early_express.payment_service.domain.payment.infrastructure.client.toss.TossPaymentsClient;
import com.early_express.payment_service.domain.payment.domain.exception.PaymentVerificationException;
import com.early_express.payment_service.domain.payment.domain.exception.RefundException;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossCancelRequest;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossCancelResponse;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossPaymentVerifyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TossPaymentsClient 테스트
 * Mockito를 사용하여 외부 API 모킹
 */
@SpringBootTest
@DisplayName("TossPaymentsClient 테스트")
class TossPaymentsClientTest {

    @MockBean
    private TossPaymentsClient tossPaymentsClient;

    @Test
    @DisplayName("결제 조회 성공 - DONE 상태의 결제 정보를 반환한다")
    void getPayment_Success() {
        // given
        String paymentKey = "test-payment-key-123";

        TossPaymentVerifyResponse mockResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(paymentKey)
                .orderId("ORDER-001")
                .status("DONE")
                .totalAmount(new BigDecimal("10000"))
                .balanceAmount(new BigDecimal("10000"))
                .suppliedAmount(new BigDecimal("9091"))
                .vat(new BigDecimal("909"))
                .approvedAt(LocalDateTime.of(2024, 1, 15, 10, 30))
                .requestedAt(LocalDateTime.of(2024, 1, 15, 10, 29, 30))
                .transactionKey("tx-123456")
                .method("카드")
                .build();

        given(tossPaymentsClient.getPayment(paymentKey))
                .willReturn(mockResponse);

        // when
        TossPaymentVerifyResponse response = tossPaymentsClient.getPayment(paymentKey);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentKey()).isEqualTo(paymentKey);
        assertThat(response.getOrderId()).isEqualTo("ORDER-001");
        assertThat(response.getStatus()).isEqualTo("DONE");
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("10000"));
        assertThat(response.isDone()).isTrue();
    }

    @Test
    @DisplayName("결제 조회 실패 - 404 Not Found")
    void getPayment_NotFound() {
        // given
        String paymentKey = "non-existent-key";

        given(tossPaymentsClient.getPayment(paymentKey))
                .willThrow(new PaymentVerificationException(
                        null,
                        "결제 정보를 찾을 수 없습니다."
                ));

        // when & then
        assertThatThrownBy(() -> tossPaymentsClient.getPayment(paymentKey))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    @Test
    @DisplayName("결제 조회 실패 - 401 Unauthorized (인증 실패)")
    void getPayment_Unauthorized() {
        // given
        String paymentKey = "test-payment-key";

        given(tossPaymentsClient.getPayment(paymentKey))
                .willThrow(new PaymentVerificationException(
                        null,
                        "토스페이먼츠 인증에 실패했습니다."
                ));

        // when & then
        assertThatThrownBy(() -> tossPaymentsClient.getPayment(paymentKey))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("인증");
    }

    @Test
    @DisplayName("결제 조회 실패 - 500 Internal Server Error")
    void getPayment_ServerError() {
        // given
        String paymentKey = "test-payment-key";

        given(tossPaymentsClient.getPayment(paymentKey))
                .willThrow(new PaymentVerificationException(
                        null,
                        "토스페이먼츠 시스템 오류가 발생했습니다."
                ));

        // when & then
        assertThatThrownBy(() -> tossPaymentsClient.getPayment(paymentKey))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("시스템 오류");
    }

    @Test
    @DisplayName("결제 취소 성공 - 전액 취소 처리")
    void cancelPayment_FullCancel_Success() {
        // given
        String paymentKey = "test-payment-key-456";
        TossCancelRequest request = TossCancelRequest.fullCancel("고객 요청");

        TossCancelResponse.CancelDetail cancelDetail = new TossCancelResponse.CancelDetail(
                new BigDecimal("10000"),
                "고객 요청",
                LocalDateTime.of(2024, 1, 15, 11, 0),
                "refund-tx-123"
        );

        TossCancelResponse mockResponse = TossCancelResponse.builder()
                .paymentKey(paymentKey)
                .orderId("ORDER-002")
                .status("CANCELED")
                .totalAmount(new BigDecimal("10000"))
                .balanceAmount(BigDecimal.ZERO)
                .cancels(List.of(cancelDetail))
                .build();

        given(tossPaymentsClient.cancelPayment(eq(paymentKey), any(TossCancelRequest.class)))
                .willReturn(mockResponse);

        // when
        TossCancelResponse response = tossPaymentsClient.cancelPayment(paymentKey, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("CANCELED");
        assertThat(response.isFullyCanceled()).isTrue();
        assertThat(response.getCancels()).hasSize(1);
        assertThat(response.getCancels().get(0).getCancelAmount())
                .isEqualTo(new BigDecimal("10000"));
        assertThat(response.getCancels().get(0).getTransactionKey())
                .isEqualTo("refund-tx-123");
    }

    @Test
    @DisplayName("결제 취소 실패 - 400 Bad Request (잘못된 요청)")
    void cancelPayment_BadRequest() {
        // given
        String paymentKey = "test-payment-key";
        TossCancelRequest request = TossCancelRequest.fullCancel("취소 사유");

        given(tossPaymentsClient.cancelPayment(eq(paymentKey), any(TossCancelRequest.class)))
                .willThrow(new RefundException(
                        null,
                        "올바르지 않은 취소 요청입니다."
                ));

        // when & then
        assertThatThrownBy(() -> tossPaymentsClient.cancelPayment(paymentKey, request))
                .isInstanceOf(RefundException.class)
                .hasMessageContaining("올바르지 않은 취소 요청");
    }

    @Test
    @DisplayName("결제 취소 실패 - 409 Conflict (취소 불가능한 상태)")
    void cancelPayment_Conflict() {
        // given
        String paymentKey = "test-payment-key";
        TossCancelRequest request = TossCancelRequest.fullCancel("취소 사유");

        given(tossPaymentsClient.cancelPayment(eq(paymentKey), any(TossCancelRequest.class)))
                .willThrow(new RefundException(
                        null,
                        "취소할 수 없는 결제 상태입니다."
                ));

        // when & then
        assertThatThrownBy(() -> tossPaymentsClient.cancelPayment(paymentKey, request))
                .isInstanceOf(RefundException.class)
                .hasMessageContaining("취소할 수 없는");
    }

    @Test
    @DisplayName("결제 취소 실패 - 404 Not Found")
    void cancelPayment_NotFound() {
        // given
        String paymentKey = "non-existent-key";
        TossCancelRequest request = TossCancelRequest.fullCancel("취소 사유");

        given(tossPaymentsClient.cancelPayment(eq(paymentKey), any(TossCancelRequest.class)))
                .willThrow(new RefundException(
                        null,
                        "결제 정보를 찾을 수 없습니다."
                ));

        // when & then
        assertThatThrownBy(() -> tossPaymentsClient.cancelPayment(paymentKey, request))
                .isInstanceOf(RefundException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    @Test
    @DisplayName("결제 조회 - 취소된 결제 확인")
    void getPayment_CanceledStatus() {
        // given
        String paymentKey = "canceled-payment-key";

        TossPaymentVerifyResponse mockResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(paymentKey)
                .orderId("ORDER-003")
                .status("CANCELED")
                .totalAmount(new BigDecimal("10000"))
                .balanceAmount(BigDecimal.ZERO)
                .approvedAt(LocalDateTime.of(2024, 1, 15, 10, 30))
                .transactionKey("tx-789")
                .build();

        given(tossPaymentsClient.getPayment(paymentKey))
                .willReturn(mockResponse);

        // when
        TossPaymentVerifyResponse response = tossPaymentsClient.getPayment(paymentKey);

        // then
        assertThat(response.getStatus()).isEqualTo("CANCELED");
        assertThat(response.isCanceled()).isTrue();
        assertThat(response.isDone()).isFalse();
    }

    @Test
    @DisplayName("결제 조회 - 실패한 결제 확인")
    void getPayment_FailedStatus() {
        // given
        String paymentKey = "failed-payment-key";

        TossPaymentVerifyResponse mockResponse = TossPaymentVerifyResponse.builder()
                .paymentKey(paymentKey)
                .orderId("ORDER-004")
                .status("ABORTED")
                .totalAmount(new BigDecimal("10000"))
                .requestedAt(LocalDateTime.of(2024, 1, 15, 10, 29, 30))
                .failure(new TossPaymentVerifyResponse.TossFailure(
                        "PAY_PROCESS_ABORTED",
                        "결제 승인에 실패했습니다."
                ))
                .build();

        given(tossPaymentsClient.getPayment(paymentKey))
                .willReturn(mockResponse);

        // when
        TossPaymentVerifyResponse response = tossPaymentsClient.getPayment(paymentKey);

        // then
        assertThat(response.getStatus()).isEqualTo("ABORTED");
        assertThat(response.isFailed()).isTrue();
        assertThat(response.getFailure()).isNotNull();
        assertThat(response.getFailure().getCode()).isEqualTo("PAY_PROCESS_ABORTED");
    }

    @Test
    @DisplayName("부분 취소 응답 처리")
    void cancelPayment_PartialCancel() {
        // given
        String paymentKey = "partial-cancel-key";
        TossCancelRequest request = TossCancelRequest.fullCancel("부분 환불");

        TossCancelResponse.CancelDetail cancelDetail = new TossCancelResponse.CancelDetail(
                new BigDecimal("5000"),
                "부분 환불",
                LocalDateTime.of(2024, 1, 15, 11, 30),
                "refund-tx-456"
        );

        TossCancelResponse mockResponse = TossCancelResponse.builder()
                .paymentKey(paymentKey)
                .orderId("ORDER-005")
                .status("PARTIAL_CANCELED")
                .totalAmount(new BigDecimal("10000"))
                .balanceAmount(new BigDecimal("5000"))
                .cancels(List.of(cancelDetail))
                .build();

        given(tossPaymentsClient.cancelPayment(eq(paymentKey), any(TossCancelRequest.class)))
                .willReturn(mockResponse);

        // when
        TossCancelResponse response = tossPaymentsClient.cancelPayment(paymentKey, request);

        // then
        assertThat(response.getStatus()).isEqualTo("PARTIAL_CANCELED");
        assertThat(response.isPartiallyCanceled()).isTrue();
        assertThat(response.isFullyCanceled()).isFalse();
        assertThat(response.getBalanceAmount()).isEqualTo(new BigDecimal("5000"));
    }
}