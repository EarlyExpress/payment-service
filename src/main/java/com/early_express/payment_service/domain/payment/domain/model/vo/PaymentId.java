package com.early_express.payment_service.domain.payment.domain.model.vo;

import com.early_express.payment_service.global.common.utils.UuidUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

/**
 * 결제 ID Value Object
 * 불변 객체로 결제의 고유 식별자를 표현
 */
@Getter
@EqualsAndHashCode
public class PaymentId {

    private final String value;

    private PaymentId(String value) {
        validateNotNull(value);
        this.value = value;
    }

    /**
     * 새로운 PaymentId 생성
     */
    public static PaymentId create() {
        return new PaymentId(UuidUtils.generate().toString());
    }

    /**
     * 기존 ID로부터 PaymentId 생성
     */
    public static PaymentId from(String value) {
        return new PaymentId(value);
    }

    private void validateNotNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("결제 ID는 null이거나 빈 값일 수 없습니다.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}