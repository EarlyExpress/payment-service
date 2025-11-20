package com.early_express.payment_service.domain.payment.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 상태 Enum
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    /**
     * 검증 대기 중
     */
    PENDING("검증 대기 중"),

    /**
     * 검증 중
     */
    VERIFYING("검증 중"),

    /**
     * 검증 완료 (정상 결제)
     */
    VERIFIED("검증 완료"),

    /**
     * 검증 실패
     */
    VERIFICATION_FAILED("검증 실패"),

    /**
     * 환불 처리 중
     */
    REFUNDING("환불 처리 중"),

    /**
     * 부분 환불됨
     */
    PARTIALLY_REFUNDED("부분 환불됨"),

    /**
     * 전액 환불됨
     */
    REFUNDED("전액 환불됨"),

    /**
     * 환불 실패
     */
    REFUND_FAILED("환불 실패"),

    /**
     * 취소됨
     */
    CANCELLED("취소됨");

    private final String description;

    /**
     * 환불 가능한 상태인지 확인
     */
    public boolean isRefundable() {
        return this == VERIFIED || this == PARTIALLY_REFUNDED;
    }

    /**
     * 최종 상태인지 확인
     */
    public boolean isFinalState() {
        return this == VERIFICATION_FAILED
                || this == REFUNDED
                || this == REFUND_FAILED
                || this == CANCELLED;
    }

    /**
     * 검증 완료 상태인지 확인
     */
    public boolean isVerified() {
        return this == VERIFIED;
    }

    /**
     * 환불 처리 중인지 확인
     */
    public boolean isRefunding() {
        return this == REFUNDING;
    }
}