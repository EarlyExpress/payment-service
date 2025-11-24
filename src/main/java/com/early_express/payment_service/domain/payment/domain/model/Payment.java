package com.early_express.payment_service.domain.payment.domain.model;

import com.early_express.payment_service.domain.payment.domain.exception.PaymentErrorCode;
import com.early_express.payment_service.domain.payment.domain.exception.PaymentException;
import com.early_express.payment_service.domain.payment.domain.exception.PaymentVerificationException;
import com.early_express.payment_service.domain.payment.domain.exception.RefundException;
import com.early_express.payment_service.domain.payment.domain.model.vo.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Aggregate Root
 * 결제의 전체 생명주기를 관리하는 핵심 도메인 모델
 */
@Getter
public class Payment {

    private final PaymentId id;
    private final String orderId; // Order ID 참조

    // 금액 정보
    private PaymentAmountInfo amountInfo;

    // PG 정보
    private PgInfo pgInfo;

    // 결제자 정보
    private final PayerInfo payerInfo;

    // 수취인 정보
    private final PayeeInfo payeeInfo;

    // 상태
    private PaymentStatus status;
    private String verificationStatus; // PENDING, SUCCESS, FAILED
    private String verificationFailReason;

    // 생성 정보
    private final LocalDateTime createdAt;

    // 검증 정보
    private LocalDateTime verifiedAt;

    // 환불 정보
    private LocalDateTime refundedAt;
    private String refundReason;
    
    // 베이스엔티티 매핑용
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private boolean isDeleted;

    @Builder
    private Payment(
            PaymentId id,
            String orderId,
            PaymentAmountInfo amountInfo,
            PgInfo pgInfo,
            PayerInfo payerInfo,
            PayeeInfo payeeInfo,
            PaymentStatus status,
            String verificationStatus,
            String verificationFailReason,
            LocalDateTime createdAt,
            LocalDateTime verifiedAt,
            LocalDateTime refundedAt,
            String refundReason) {

        this.id = id;
        this.orderId = orderId;
        this.amountInfo = amountInfo;
        this.pgInfo = pgInfo;
        this.payerInfo = payerInfo;
        this.payeeInfo = payeeInfo;
        this.status = status;
        this.verificationStatus = verificationStatus;
        this.verificationFailReason = verificationFailReason;
        this.createdAt = createdAt;
        this.verifiedAt = verifiedAt;
        this.refundedAt = refundedAt;
        this.refundReason = refundReason;
    }

    /**
     * 새로운 결제 생성 (검증 후)
     */
    public static Payment create(
            String orderId,
            BigDecimal amount,
            PgInfo pgInfo,
            PayerInfo payerInfo,
            PayeeInfo payeeInfo) {

        return Payment.builder()
                .id(null)
                .orderId(orderId)
                .amountInfo(PaymentAmountInfo.of(amount))
                .pgInfo(pgInfo)
                .payerInfo(payerInfo)
                .payeeInfo(payeeInfo)
                .status(PaymentStatus.PENDING)
                .verificationStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== 결제 검증 관련 메서드 =====

    /**
     * 검증 시작
     */
    public void startVerification() {
        validateStatus(PaymentStatus.PENDING, "검증 시작");
        this.status = PaymentStatus.VERIFYING;
    }

    /**
     * 검증 성공
     */
    public void verifySuccess() {
        validateStatus(PaymentStatus.VERIFYING, "검증 완료");

        // PG 승인 시간 검증
        if (!this.pgInfo.isApprovalTimeValid()) {
            throw new PaymentVerificationException(
                    PaymentErrorCode.PAYMENT_EXPIRED,
                    "PG 승인 시간이 1시간을 초과했습니다."
            );
        }

        this.status = PaymentStatus.VERIFIED;
        this.verificationStatus = "SUCCESS";
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * 검증 실패
     */
    public void verifyFailed(String failReason) {
        validateStatus(PaymentStatus.VERIFYING, "검증 실패");

        this.status = PaymentStatus.VERIFICATION_FAILED;
        this.verificationStatus = "FAILED";
        this.verificationFailReason = failReason;
    }

    /**
     * 결제 금액 검증
     */
    public void validateAmount(BigDecimal expectedAmount) {
        if (!this.amountInfo.matchesAmount(expectedAmount)) {
            throw new PaymentVerificationException(
                    PaymentErrorCode.AMOUNT_MISMATCH,
                    String.format("결제 금액이 일치하지 않습니다. PG 금액: %s, 예상 금액: %s",
                            this.amountInfo.getAmount(),
                            expectedAmount)
            );
        }
    }

    // ===== 환불 관련 메서드 =====

    /**
     * 환불 시작
     */
    public void startRefund(BigDecimal refundAmount, String refundReason) {
        // 환불 가능 상태 검증
        if (!this.status.isRefundable()) {
            throw new RefundException(
                    PaymentErrorCode.REFUND_NOT_ALLOWED,
                    "환불이 불가능한 상태입니다: " + this.status.getDescription()
            );
        }

        // 환불 가능 금액 검증
        if (refundAmount.compareTo(this.amountInfo.getRefundableAmount()) > 0) {
            throw new RefundException(
                    PaymentErrorCode.REFUND_AMOUNT_EXCEEDS_PAYMENT,
                    String.format("환불 금액이 환불 가능 금액을 초과합니다. 요청 금액: %s, 환불 가능 금액: %s",
                            refundAmount,
                            this.amountInfo.getRefundableAmount())
            );
        }

        this.status = PaymentStatus.REFUNDING;
        this.refundReason = refundReason;
    }

    /**
     * 환불 완료
     */
    public void completeRefund(BigDecimal refundAmount, String pgRefundId) {
        validateStatus(PaymentStatus.REFUNDING, "환불 완료");

        // 금액 정보 업데이트
        this.amountInfo = this.amountInfo.withRefund(refundAmount);

        // PG 정보 업데이트
        this.pgInfo = this.pgInfo.withRefund(pgRefundId, LocalDateTime.now());

        // 상태 업데이트
        if (this.amountInfo.isFullyRefunded()) {
            this.status = PaymentStatus.REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }

        this.refundedAt = LocalDateTime.now();
    }

    /**
     * 환불 실패
     */
    public void failRefund(String failReason) {
        validateStatus(PaymentStatus.REFUNDING, "환불 실패");

        this.status = PaymentStatus.REFUND_FAILED;
        this.verificationFailReason = failReason;
    }

    /**
     * 결제 취소
     */
    public void cancel(String cancelReason) {
        if (this.status.isFinalState()) {
            throw new PaymentException(
                    PaymentErrorCode.PAYMENT_CANNOT_BE_CANCELLED,
                    "최종 상태에서는 취소할 수 없습니다: " + this.status.getDescription()
            );
        }

        this.status = PaymentStatus.CANCELLED;
        this.refundReason = cancelReason;
    }

    // ===== 검증 메서드 =====

    /**
     * 상태 검증
     */
    private void validateStatus(PaymentStatus expectedStatus, String operation) {
        if (this.status != expectedStatus) {
            throw new PaymentException(
                    PaymentErrorCode.INVALID_PAYMENT_STATUS,
                    String.format("%s는 %s 상태에서만 가능합니다. 현재 상태: %s",
                            operation,
                            expectedStatus.getDescription(),
                            this.status.getDescription())
            );
        }
    }

    // ===== 조회 메서드 =====

    /**
     * 검증 완료 여부 확인
     */
    public boolean isVerified() {
        return this.status == PaymentStatus.VERIFIED;
    }

    /**
     * 환불 가능 여부 확인
     */
    public boolean isRefundable() {
        return this.status.isRefundable() && this.amountInfo.isRefundable();
    }

    /**
     * 전액 환불 여부 확인
     */
    public boolean isFullyRefunded() {
        return this.status == PaymentStatus.REFUNDED;
    }

    /**
     * 부분 환불 여부 확인
     */
    public boolean isPartiallyRefunded() {
        return this.status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    /**
     * 결제 ID 문자열 반환
     */
    public String getIdValue() {
        return this.id.getValue();
    }

    /**
     * 결제 금액 반환
     */
    public BigDecimal getAmount() {
        return this.amountInfo.getAmount();
    }

    /**
     * 환불된 금액 반환
     */
    public BigDecimal getRefundedAmount() {
        return this.amountInfo.getRefundedAmount();
    }

    /**
     * 환불 가능 금액 반환
     */
    public BigDecimal getRefundableAmount() {
        return this.amountInfo.getRefundableAmount();
    }
}