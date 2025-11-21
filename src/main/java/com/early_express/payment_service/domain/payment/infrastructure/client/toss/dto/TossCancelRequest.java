package com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Toss Payments 결제 취소 요청 DTO
 * POST /v1/payments/{paymentKey}/cancel
 *
 * - cancelReason만 필수
 * - cancelAmount 없으면 전액 취소
 */
@Getter
@Builder
public class TossCancelRequest {

    /**
     * 취소 사유 (필수)
     */
    @JsonProperty("cancelReason")
    private String cancelReason;

    /**
     * 전액 취소 요청 생성
     */
    public static TossCancelRequest fullCancel(String cancelReason) {
        return TossCancelRequest.builder()
                .cancelReason(cancelReason)
                .build();
    }
}
