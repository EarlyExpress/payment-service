package com.early_express.payment_service.domain.payment.infrastructure.client.toss;

import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossCancelRequest;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossCancelResponse;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossPaymentVerifyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Toss Payments Feign Client
 * 토스페이먼츠 API와의 동기 통신
 */
@FeignClient(
        name = "toss-payments",
        url = "${client.toss-payments.url:https://api.tosspayments.com}",
        configuration = TossPaymentsClientConfig.class
)
public interface TossPaymentsClient {

    /**
     * 결제 조회 (검증용)
     * GET /v1/payments/{paymentKey}
     *
     * @param paymentKey 토스 결제 키
     * @return 결제 정보
     */
    @GetMapping("/v1/payments/{paymentKey}")
    TossPaymentVerifyResponse getPayment(
            @PathVariable("paymentKey") String paymentKey
    );

    /**
     * 결제 취소
     * POST /v1/payments/{paymentKey}/cancel
     *
     * @param paymentKey 토스 결제 키
     * @param request 취소 요청
     * @return 취소 결과
     */
    @PostMapping("/v1/payments/{paymentKey}/cancel")
    TossCancelResponse cancelPayment(
            @PathVariable("paymentKey") String paymentKey,
            @RequestBody TossCancelRequest request
    );
}
