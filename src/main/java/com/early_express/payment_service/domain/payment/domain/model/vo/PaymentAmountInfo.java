package com.early_express.payment_service.domain.payment.domain.model.vo;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 결제 금액 정보 Value Object
 */
@Getter
@EqualsAndHashCode
public class PaymentAmountInfo {

    private final BigDecimal amount;
    private final BigDecimal refundedAmount; // 환불된 금액
    private final String currency; // 통화 (KRW, USD 등)

    @Builder
    private PaymentAmountInfo(
            BigDecimal amount,
            BigDecimal refundedAmount,
            String currency) {

        validateAmount(amount);

        this.amount = amount;
        this.refundedAmount = refundedAmount != null ? refundedAmount : BigDecimal.ZERO;
        this.currency = currency != null ? currency : "KRW";
    }

    /**
     * 새로운 결제 금액 정보 생성
     */
    public static PaymentAmountInfo of(BigDecimal amount) {
        return PaymentAmountInfo.builder()
                .amount(amount)
                .build();
    }

    /**
     * 환불 금액 추가
     */
    public PaymentAmountInfo withRefund(BigDecimal refundAmount) {
        validateRefundAmount(refundAmount);

        BigDecimal newRefundedAmount = this.refundedAmount.add(refundAmount);

        if (newRefundedAmount.compareTo(this.amount) > 0) {
            throw new IllegalArgumentException("환불 금액이 결제 금액을 초과할 수 없습니다.");
        }

        return PaymentAmountInfo.builder()
                .amount(this.amount)
                .refundedAmount(newRefundedAmount)
                .currency(this.currency)
                .build();
    }

    /**
     * 환불 가능 금액 조회
     */
    public BigDecimal getRefundableAmount() {
        return this.amount.subtract(this.refundedAmount);
    }

    /**
     * 전액 환불 여부 확인
     */
    public boolean isFullyRefunded() {
        return this.refundedAmount.compareTo(this.amount) == 0;
    }

    /**
     * 부분 환불 여부 확인
     */
    public boolean isPartiallyRefunded() {
        return this.refundedAmount.compareTo(BigDecimal.ZERO) > 0
                && this.refundedAmount.compareTo(this.amount) < 0;
    }

    /**
     * 환불 가능 여부 확인
     */
    public boolean isRefundable() {
        return this.refundedAmount.compareTo(this.amount) < 0;
    }

    /**
     * 금액 일치 여부 확인
     */
    public boolean matchesAmount(BigDecimal otherAmount) {
        return this.amount.compareTo(otherAmount) == 0;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("결제 금액은 null일 수 없습니다.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
    }

    private void validateRefundAmount(BigDecimal refundAmount) {
        if (refundAmount == null) {
            throw new IllegalArgumentException("환불 금액은 null일 수 없습니다.");
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
    }
}