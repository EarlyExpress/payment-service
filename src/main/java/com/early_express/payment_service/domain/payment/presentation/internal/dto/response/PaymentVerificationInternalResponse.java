package com.early_express.payment_service.domain.payment.presentation.internal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 검증 및 등록 응답 DTO
 * Payment Service → Order Service (Internal)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationInternalResponse {

    /**
     * Payment ID (생성된 결제 엔티티 ID)
     */
    private String paymentId;

    /**
     * 주문 ID
     */
    private String orderId;

    /**
     * 검증 상태 (VERIFIED, FAILED)
     */
    private String status;

    /**
     * PG 거래 ID
     */
    private String pgTransactionId;

    /**
     * 검증된 금액
     */
    private BigDecimal verifiedAmount;

    /**
     * PG 승인 시간
     */
    private LocalDateTime pgApprovedAt;

    /**
     * 검증 완료 시간
     */
    private LocalDateTime verifiedAt;

    /**
     * 메시지
     */
    private String message;
}