package com.early_express.payment_service.domain.payment.presentation.internal;

import com.early_express.payment_service.domain.payment.application.service.PaymentService;
import com.early_express.payment_service.domain.payment.domain.model.Payment;
import com.early_express.payment_service.domain.payment.presentation.internal.dto.request.PaymentVerificationInternalRequest;
import com.early_express.payment_service.domain.payment.presentation.internal.dto.response.PaymentVerificationInternalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment Internal API Controller
 * 다른 서비스(Order Service)에서 호출하는 내부 API
 * Endpoint Pattern: /v1/payment/internal/all/{resource}
 */
@Slf4j
@RestController
@RequestMapping("/v1/payment/internal/all")
@RequiredArgsConstructor
public class PaymentInternalController {

    private final PaymentService paymentService;

    /**
     * 결제 검증 및 등록 (Saga Step 2)
     * POST /v1/payment/internal/all/verify-and-register
     * Order Service에서 호출
     * - PG사 결제 검증
     * - Payment 엔티티 생성
     * - 성공 시: PaymentVerifiedEvent 발행
     */
    @PostMapping("/verify-and-register")
    public ResponseEntity<PaymentVerificationInternalResponse> verifyAndRegisterPayment(
            @Valid @RequestBody PaymentVerificationInternalRequest request) {

        log.info("결제 검증 요청 수신 - orderId: {}, pgPaymentId: {}",
                request.getOrderId(), request.getPgPaymentId());

        Payment payment = paymentService.verifyAndRegisterPayment(
                request.getOrderId(),
                request.getPgProvider(),
                request.getPgPaymentId(),
                request.getPgPaymentKey(),
                request.getExpectedAmount(),
                request.getPayerCompanyId(),
                request.getPayerName(),
                request.getPayerEmail(),
                request.getPayerPhone(),
                request.getPayeeCompanyId(),
                request.getPayeeName()
        );

        PaymentVerificationInternalResponse response = PaymentVerificationInternalResponse.builder()
                .paymentId(payment.getIdValue())
                .orderId(payment.getOrderId())
                .status(payment.getStatus().name())
                .pgTransactionId(payment.getPgInfo().getPgTransactionId())
                .verifiedAmount(payment.getAmount())
                .pgApprovedAt(payment.getPgInfo().getPgApprovedAt())
                .verifiedAt(payment.getVerifiedAt())
                .message("결제 검증이 완료되었습니다.")
                .build();

        log.info("결제 검증 완료 - paymentId: {}, orderId: {}",
                payment.getIdValue(), payment.getOrderId());

        return ResponseEntity.ok(response);
    }

    /**
     * Payment ID로 결제 조회
     * GET /v1/payment/internal/all/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentVerificationInternalResponse> getPayment(
            @PathVariable String paymentId) {

        log.info("결제 조회 요청 - paymentId: {}", paymentId);

        Payment payment = paymentService.findById(paymentId);

        PaymentVerificationInternalResponse response = PaymentVerificationInternalResponse.builder()
                .paymentId(payment.getIdValue())
                .orderId(payment.getOrderId())
                .status(payment.getStatus().name())
                .pgTransactionId(payment.getPgInfo().getPgTransactionId())
                .verifiedAmount(payment.getAmount())
                .pgApprovedAt(payment.getPgInfo().getPgApprovedAt())
                .verifiedAt(payment.getVerifiedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Order ID로 결제 조회
     * GET /v1/payment/internal/all/by-order/{orderId}
     */
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<PaymentVerificationInternalResponse> getPaymentByOrderId(
            @PathVariable String orderId) {

        log.info("주문별 결제 조회 요청 - orderId: {}", orderId);

        Payment payment = paymentService.findByOrderId(orderId);

        PaymentVerificationInternalResponse response = PaymentVerificationInternalResponse.builder()
                .paymentId(payment.getIdValue())
                .orderId(payment.getOrderId())
                .status(payment.getStatus().name())
                .pgTransactionId(payment.getPgInfo().getPgTransactionId())
                .verifiedAmount(payment.getAmount())
                .pgApprovedAt(payment.getPgInfo().getPgApprovedAt())
                .verifiedAt(payment.getVerifiedAt())
                .build();

        return ResponseEntity.ok(response);
    }
}