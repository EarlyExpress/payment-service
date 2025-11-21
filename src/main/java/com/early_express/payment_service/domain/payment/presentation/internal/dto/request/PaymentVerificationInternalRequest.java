package com.early_express.payment_service.domain.payment.presentation.internal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 결제 검증 및 등록 요청 DTO
 * Order Service → Payment Service (Internal)
 */
@Getter
@Builder
public class PaymentVerificationInternalRequest {

    @NotBlank(message = "주문 ID는 필수입니다.")
    private String orderId;

    @NotBlank(message = "PG 제공자는 필수입니다.")
    private String pgProvider;

    @NotBlank(message = "PG 결제 ID는 필수입니다.")
    private String pgPaymentId;

    @NotBlank(message = "PG 결제 키는 필수입니다.")
    private String pgPaymentKey;

    @NotNull(message = "예상 금액은 필수입니다.")
    private BigDecimal expectedAmount;

    @NotBlank(message = "지불자 업체 ID는 필수입니다.")
    private String payerCompanyId;

    @NotBlank(message = "지불자 이름은 필수입니다.")
    private String payerName;

    private String payerEmail;
    private String payerPhone;

    @NotBlank(message = "수취인 업체 ID는 필수입니다.")
    private String payeeCompanyId;

    @NotBlank(message = "수취인 이름은 필수입니다.")
    private String payeeName;
}