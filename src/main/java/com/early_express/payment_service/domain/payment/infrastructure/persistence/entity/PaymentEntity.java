package com.early_express.payment_service.domain.payment.infrastructure.persistence.entity;

import com.early_express.payment_service.domain.payment.domain.model.Payment;
import com.early_express.payment_service.domain.payment.domain.model.PaymentStatus;
import com.early_express.payment_service.domain.payment.domain.model.vo.*;
import com.early_express.payment_service.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment JPA Entity
 */
@Entity
@Table(name = "p_payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_id", nullable = false, unique = true, length = 36)
    private String orderId;

    // ===== 금액 정보 =====
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "refunded_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    // ===== PG 정보 =====
    @Column(name = "pg_provider", nullable = false, length = 20)
    private String pgProvider;

    @Column(name = "pg_payment_id", nullable = false, length = 200)
    private String pgPaymentId;

    @Column(name = "pg_payment_key", length = 200)
    private String pgPaymentKey;

    @Column(name = "pg_transaction_id", length = 200)
    private String pgTransactionId;

    @Column(name = "pg_refund_id", length = 200)
    private String pgRefundId;

    @Column(name = "pg_approved_at")
    private LocalDateTime pgApprovedAt;

    @Column(name = "pg_refunded_at")
    private LocalDateTime pgRefundedAt;

    // ===== 결제자 정보 =====
    @Column(name = "payer_company_id", nullable = false, length = 36)
    private String payerCompanyId;

    @Column(name = "payer_name", nullable = false, length = 100)
    private String payerName;

    @Column(name = "payer_email", length = 100)
    private String payerEmail;

    @Column(name = "payer_phone", length = 20)
    private String payerPhone;

    // ===== 수취인 정보 =====
    @Column(name = "payee_company_id", nullable = false, length = 36)
    private String payeeCompanyId;

    @Column(name = "payee_name", nullable = false, length = 100)
    private String payeeName;

    // ===== 상태 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus;

    @Column(name = "verification_fail_reason", columnDefinition = "TEXT")
    private String verificationFailReason;

    // ===== 검증 및 환불 정보 =====
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Builder
    private PaymentEntity(
            String id,
            String orderId,
            BigDecimal amount,
            BigDecimal refundedAmount,
            String currency,
            String pgProvider,
            String pgPaymentId,
            String pgPaymentKey,
            String pgTransactionId,
            String pgRefundId,
            LocalDateTime pgApprovedAt,
            LocalDateTime pgRefundedAt,
            String payerCompanyId,
            String payerName,
            String payerEmail,
            String payerPhone,
            String payeeCompanyId,
            String payeeName,
            PaymentStatus status,
            String verificationStatus,
            String verificationFailReason,
            LocalDateTime verifiedAt,
            LocalDateTime refundedAt,
            String refundReason) {

        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.refundedAmount = refundedAmount;
        this.currency = currency;
        this.pgProvider = pgProvider;
        this.pgPaymentId = pgPaymentId;
        this.pgPaymentKey = pgPaymentKey;
        this.pgTransactionId = pgTransactionId;
        this.pgRefundId = pgRefundId;
        this.pgApprovedAt = pgApprovedAt;
        this.pgRefundedAt = pgRefundedAt;
        this.payerCompanyId = payerCompanyId;
        this.payerName = payerName;
        this.payerEmail = payerEmail;
        this.payerPhone = payerPhone;
        this.payeeCompanyId = payeeCompanyId;
        this.payeeName = payeeName;
        this.status = status;
        this.verificationStatus = verificationStatus;
        this.verificationFailReason = verificationFailReason;
        this.verifiedAt = verifiedAt;
        this.refundedAt = refundedAt;
        this.refundReason = refundReason;
    }

    /**
     * 도메인 모델로부터 엔티티 생성
     */
    public static PaymentEntity fromDomain(Payment payment) {
        return PaymentEntity.builder()
                .id(payment.getIdValue())
                .orderId(payment.getOrderId())
                .amount(payment.getAmountInfo().getAmount())
                .refundedAmount(payment.getAmountInfo().getRefundedAmount())
                .currency(payment.getAmountInfo().getCurrency())
                .pgProvider(payment.getPgInfo().getPgProvider())
                .pgPaymentId(payment.getPgInfo().getPgPaymentId())
                .pgPaymentKey(payment.getPgInfo().getPgPaymentKey())
                .pgTransactionId(payment.getPgInfo().getPgTransactionId())
                .pgRefundId(payment.getPgInfo().getPgRefundId())
                .pgApprovedAt(payment.getPgInfo().getPgApprovedAt())
                .pgRefundedAt(payment.getPgInfo().getPgRefundedAt())
                .payerCompanyId(payment.getPayerInfo().getPayerCompanyId())
                .payerName(payment.getPayerInfo().getPayerName())
                .payerEmail(payment.getPayerInfo().getPayerEmail())
                .payerPhone(payment.getPayerInfo().getPayerPhone())
                .payeeCompanyId(payment.getPayeeInfo().getPayeeCompanyId())
                .payeeName(payment.getPayeeInfo().getPayeeName())
                .status(payment.getStatus())
                .verificationStatus(payment.getVerificationStatus())
                .verificationFailReason(payment.getVerificationFailReason())
                .verifiedAt(payment.getVerifiedAt())
                .refundedAt(payment.getRefundedAt())
                .refundReason(payment.getRefundReason())
                .build();
    }

    /**
     * 엔티티를 도메인 모델로 변환
     */
    public Payment toDomain() {
        return Payment.builder()
                .id(PaymentId.from(this.id))
                .orderId(this.orderId)
                .amountInfo(PaymentAmountInfo.builder()
                        .amount(this.amount)
                        .refundedAmount(this.refundedAmount)
                        .currency(this.currency)
                        .build())
                .pgInfo(PgInfo.builder()
                        .pgProvider(this.pgProvider)
                        .pgPaymentId(this.pgPaymentId)
                        .pgPaymentKey(this.pgPaymentKey)
                        .pgTransactionId(this.pgTransactionId)
                        .pgRefundId(this.pgRefundId)
                        .pgApprovedAt(this.pgApprovedAt)
                        .pgRefundedAt(this.pgRefundedAt)
                        .build())
                .payerInfo(PayerInfo.of(
                        this.payerCompanyId,
                        this.payerName,
                        this.payerEmail,
                        this.payerPhone
                ))
                .payeeInfo(PayeeInfo.of(
                        this.payeeCompanyId,
                        this.payeeName
                ))
                .status(this.status)
                .verificationStatus(this.verificationStatus)
                .verificationFailReason(this.verificationFailReason)
                .createdAt(this.getCreatedAt())
                .verifiedAt(this.verifiedAt)
                .refundedAt(this.refundedAt)
                .refundReason(this.refundReason)
                .build();
    }

    /**
     * 도메인 모델로 엔티티 업데이트
     */
    public void updateFromDomain(Payment payment) {
        this.refundedAmount = payment.getAmountInfo().getRefundedAmount();
        this.pgPaymentKey = payment.getPgInfo().getPgPaymentKey();
        this.pgTransactionId = payment.getPgInfo().getPgTransactionId();
        this.pgRefundId = payment.getPgInfo().getPgRefundId();
        this.pgRefundedAt = payment.getPgInfo().getPgRefundedAt();
        this.status = payment.getStatus();
        this.verificationStatus = payment.getVerificationStatus();
        this.verificationFailReason = payment.getVerificationFailReason();
        this.verifiedAt = payment.getVerifiedAt();
        this.refundedAt = payment.getRefundedAt();
        this.refundReason = payment.getRefundReason();
    }
}
