package com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Toss Payments 결제 취소 응답 DTO
 * POST /v1/payments/{paymentKey}/cancel
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossCancelResponse {

    @JsonProperty("paymentKey")
    private String paymentKey;

    @JsonProperty("orderId")
    private String orderId;

    /**
     * 결제 상태 (CANCELED, PARTIAL_CANCELED)
     */
    @JsonProperty("status")
    private String status;

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    /**
     * 취소 가능 금액 (남은 금액)
     */
    @JsonProperty("balanceAmount")
    private BigDecimal balanceAmount;

    /**
     * 취소 내역 목록
     */
    @JsonProperty("cancels")
    private List<CancelDetail> cancels;

    /**
     * 전액 취소 여부 확인
     */
    public boolean isFullyCanceled() {
        return "CANCELED".equals(this.status);
    }

    /**
     * 부분 취소 여부 확인
     */
    public boolean isPartiallyCanceled() {
        return "PARTIAL_CANCELED".equals(this.status);
    }

    /**
     * 취소 상세 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelDetail {

        @JsonProperty("cancelAmount")
        private BigDecimal cancelAmount;

        @JsonProperty("cancelReason")
        private String cancelReason;

        @JsonProperty("canceledAt")
        private LocalDateTime canceledAt;

        @JsonProperty("transactionKey")
        private String transactionKey;
    }
}
