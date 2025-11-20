package com.early_express.payment_service.domain.payment.domain.model.vo;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * PG 정보 Value Object
 * PG사 연동 정보를 담음
 */
@Getter
@EqualsAndHashCode
public class PgInfo {

    private final String pgProvider; // PG사 (TOSS, PORTONE 등)
    private final String pgPaymentId; // PG 결제 ID
    private final String pgPaymentKey; // PG 결제 키
    private final String pgTransactionId; // PG 거래 ID
    private final String pgRefundId; // PG 환불 ID (nullable)
    private final LocalDateTime pgApprovedAt; // PG 승인 시간
    private final LocalDateTime pgRefundedAt; // PG 환불 시간 (nullable)

    @Builder
    private PgInfo(
            String pgProvider,
            String pgPaymentId,
            String pgPaymentKey,
            String pgTransactionId,
            String pgRefundId,
            LocalDateTime pgApprovedAt,
            LocalDateTime pgRefundedAt) {

        validateNotNull(pgProvider, "PG사");
        validateNotNull(pgPaymentId, "PG 결제 ID");

        this.pgProvider = pgProvider;
        this.pgPaymentId = pgPaymentId;
        this.pgPaymentKey = pgPaymentKey;
        this.pgTransactionId = pgTransactionId;
        this.pgRefundId = pgRefundId;
        this.pgApprovedAt = pgApprovedAt;
        this.pgRefundedAt = pgRefundedAt;
    }

    /**
     * 새로운 PG 정보 생성
     */
    public static PgInfo of(
            String pgProvider,
            String pgPaymentId,
            String pgPaymentKey,
            String pgTransactionId,
            LocalDateTime pgApprovedAt) {

        return PgInfo.builder()
                .pgProvider(pgProvider)
                .pgPaymentId(pgPaymentId)
                .pgPaymentKey(pgPaymentKey)
                .pgTransactionId(pgTransactionId)
                .pgApprovedAt(pgApprovedAt)
                .build();
    }

    /**
     * 환불 정보 추가
     */
    public PgInfo withRefund(String pgRefundId, LocalDateTime pgRefundedAt) {
        return PgInfo.builder()
                .pgProvider(this.pgProvider)
                .pgPaymentId(this.pgPaymentId)
                .pgPaymentKey(this.pgPaymentKey)
                .pgTransactionId(this.pgTransactionId)
                .pgRefundId(pgRefundId)
                .pgApprovedAt(this.pgApprovedAt)
                .pgRefundedAt(pgRefundedAt)
                .build();
    }

    /**
     * 환불 처리되었는지 확인
     */
    public boolean isRefunded() {
        return pgRefundId != null && pgRefundedAt != null;
    }

    /**
     * PG 승인 시간이 유효한지 확인 (1시간 이내)
     */
    public boolean isApprovalTimeValid() {
        if (pgApprovedAt == null) {
            return false;
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return pgApprovedAt.isAfter(oneHourAgo);
    }

    private void validateNotNull(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "는 null이거나 빈 값일 수 없습니다.");
        }
    }
}