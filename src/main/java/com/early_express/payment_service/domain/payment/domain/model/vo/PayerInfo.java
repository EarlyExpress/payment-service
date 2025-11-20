package com.early_express.payment_service.domain.payment.domain.model.vo;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 결제자 정보 Value Object
 */
@Getter
@EqualsAndHashCode
public class PayerInfo {

    private final String payerCompanyId; // 결제자 업체 ID
    private final String payerName; // 결제자 이름
    private final String payerEmail; // 결제자 이메일 (nullable)
    private final String payerPhone; // 결제자 연락처 (nullable)

    @Builder
    private PayerInfo(
            String payerCompanyId,
            String payerName,
            String payerEmail,
            String payerPhone) {

        validateNotNull(payerCompanyId, "결제자 업체 ID");
        validateNotNull(payerName, "결제자 이름");

        this.payerCompanyId = payerCompanyId;
        this.payerName = payerName;
        this.payerEmail = payerEmail;
        this.payerPhone = payerPhone;
    }

    /**
     * 결제자 정보 생성
     */
    public static PayerInfo of(
            String payerCompanyId,
            String payerName,
            String payerEmail,
            String payerPhone) {

        return PayerInfo.builder()
                .payerCompanyId(payerCompanyId)
                .payerName(payerName)
                .payerEmail(payerEmail)
                .payerPhone(payerPhone)
                .build();
    }

    private void validateNotNull(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "는 null이거나 빈 값일 수 없습니다.");
        }
    }
}