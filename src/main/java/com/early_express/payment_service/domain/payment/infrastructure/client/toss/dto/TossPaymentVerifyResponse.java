package com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Toss Payments 결제 조회 응답 DTO
 * GET /v1/payments/{paymentKey}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossPaymentVerifyResponse {

    /**
     * 결제 키
     */
    @JsonProperty("paymentKey")
    private String paymentKey;

    /**
     * 주문 ID (가맹점에서 생성한 ID)
     */
    @JsonProperty("orderId")
    private String orderId;

    /**
     * 결제 상태
     * - READY: 결제 대기
     * - IN_PROGRESS: 결제 진행 중
     * - WAITING_FOR_DEPOSIT: 입금 대기
     * - DONE: 결제 완료
     * - CANCELED: 결제 취소
     * - PARTIAL_CANCELED: 부분 취소
     * - ABORTED: 결제 승인 실패
     * - EXPIRED: 결제 유효시간 만료
     */
    @JsonProperty("status")
    private String status;

    /**
     * 결제 금액
     */
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    /**
     * 취소 가능 금액
     */
    @JsonProperty("balanceAmount")
    private BigDecimal balanceAmount;

    /**
     * 공급가액
     */
    @JsonProperty("suppliedAmount")
    private BigDecimal suppliedAmount;

    /**
     * 부가세
     */
    @JsonProperty("vat")
    private BigDecimal vat;

    /**
     * 결제 승인 시간
     */
    @JsonProperty("approvedAt")
    private LocalDateTime approvedAt;

    /**
     * 결제 요청 시간
     */
    @JsonProperty("requestedAt")
    private LocalDateTime requestedAt;

    /**
     * 거래 ID
     */
    @JsonProperty("transactionKey")
    private String transactionKey;

    /**
     * 결제 수단
     * - 카드, 가상계좌, 간편결제, 휴대폰, 계좌이체, 문화상품권, 도서문화상품권, 게임문화상품권
     */
    @JsonProperty("method")
    private String method;

    /**
     * 에러 정보
     */
    @JsonProperty("failure")
    private TossFailure failure;

    /**
     * 결제 완료 여부 확인
     */
    public boolean isDone() {
        return "DONE".equals(this.status);
    }

    /**
     * 취소된 결제인지 확인
     */
    public boolean isCanceled() {
        return "CANCELED".equals(this.status) || "PARTIAL_CANCELED".equals(this.status);
    }

    /**
     * 실패한 결제인지 확인
     */
    public boolean isFailed() {
        return "ABORTED".equals(this.status) || "EXPIRED".equals(this.status);
    }

    /**
     * Toss 실패 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TossFailure {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;
    }
}
