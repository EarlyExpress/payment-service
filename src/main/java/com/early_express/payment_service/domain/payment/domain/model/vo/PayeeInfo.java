package com.early_express.payment_service.domain.payment.domain.model.vo;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 수취인 정보 Value Object
 */
@Getter
@EqualsAndHashCode
public class PayeeInfo {

    private final String payeeCompanyId; // 수취인 업체 ID
    private final String payeeName; // 수취인 이름

    @Builder
    private PayeeInfo(
            String payeeCompanyId,
            String payeeName) {

        validateNotNull(payeeCompanyId, "수취인 업체 ID");
        validateNotNull(payeeName, "수취인 이름");

        this.payeeCompanyId = payeeCompanyId;
        this.payeeName = payeeName;
    }

    /**
     * 수취인 정보 생성
     */
    public static PayeeInfo of(String payeeCompanyId, String payeeName) {
        return PayeeInfo.builder()
                .payeeCompanyId(payeeCompanyId)
                .payeeName(payeeName)
                .build();
    }

    private void validateNotNull(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "는 null이거나 빈 값일 수 없습니다.");
        }
    }
}