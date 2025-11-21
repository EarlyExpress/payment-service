package com.early_express.payment_service.domain.payment.application.service;

import com.early_express.payment_service.domain.payment.domain.messaging.*;
import com.early_express.payment_service.domain.payment.domain.exception.*;
import com.early_express.payment_service.domain.payment.domain.model.Payment;
import com.early_express.payment_service.domain.payment.domain.model.vo.*;
import com.early_express.payment_service.domain.payment.domain.repository.PaymentRepository;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.TossPaymentsClient;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossCancelRequest;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossCancelResponse;
import com.early_express.payment_service.domain.payment.infrastructure.client.toss.dto.TossPaymentVerifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Payment Application Service
 * 결제 검증, 취소, 조회 등의 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentEventPublisher eventPublisher;

    // ===== 결제 검증 및 등록 (Order Service에서 호출) =====

    /**
     * 결제 검증 및 등록
     * - PG사에서 실제 결제 검증
     * - Payment 엔티티 생성
     * - 성공 시: PaymentVerifiedEvent 발행
     * - 실패 시: PaymentVerificationFailedEvent 발행
     *
     * @throws PaymentVerificationException 검증 실패 시
     */
    @Transactional
    public Payment verifyAndRegisterPayment(
            String orderId,
            String pgProvider,
            String pgPaymentId,
            String pgPaymentKey,
            BigDecimal expectedAmount,
            String payerCompanyId,
            String payerName,
            String payerEmail,
            String payerPhone,
            String payeeCompanyId,
            String payeeName) {

        log.info("결제 검증 시작 - orderId: {}, pgProvider: {}, pgPaymentId: {}",
                orderId, pgProvider, pgPaymentId);

        // 1. 중복 검증 (멱등성 보장)
        Optional<Payment> existingPayment = paymentRepository.findByPgPaymentId(pgPaymentId);
        if (existingPayment.isPresent()) {
            log.info("이미 처리된 결제 - paymentId: {}", existingPayment.get().getIdValue());
            return existingPayment.get();
        }

        // 2. PG사 결제 조회 및 검증
        TossPaymentVerifyResponse tossResponse = verifyTossPayment(pgPaymentKey);

        // 3. PG 정보 생성
        PgInfo pgInfo = PgInfo.of(
                pgProvider,
                pgPaymentId,
                pgPaymentKey,
                tossResponse.getTransactionKey(),
                tossResponse.getApprovedAt()
        );

        // 4. 결제자/수취인 정보 생성
        PayerInfo payerInfo = PayerInfo.of(payerCompanyId, payerName, payerEmail, payerPhone);
        PayeeInfo payeeInfo = PayeeInfo.of(payeeCompanyId, payeeName);

        // 5. Payment 생성
        Payment payment = Payment.create(
                orderId,
                tossResponse.getTotalAmount(),
                pgInfo,
                payerInfo,
                payeeInfo
        );

        // 6. 검증 시작
        payment.startVerification();

        try {
            // 7. 금액 검증
            payment.validateAmount(expectedAmount);

            // 8. 검증 성공
            payment.verifySuccess();

            // 9. 저장
            Payment savedPayment = paymentRepository.save(payment);

            log.info("결제 검증 완료 - paymentId: {}, amount: {}",
                    savedPayment.getIdValue(), savedPayment.getAmount());

            return savedPayment;

        } catch (PaymentVerificationException e) {
            // 검증 실패
            payment.verifyFailed(e.getMessage());
            Payment savedPayment = paymentRepository.save(payment);

            log.error("결제 검증 실패 - paymentId: {}, reason: {}",
                    savedPayment.getIdValue(), e.getMessage());

            throw e;
        }
    }

    /**
     * Toss Payments 검증
     */
    private TossPaymentVerifyResponse verifyTossPayment(String pgPaymentKey) {
        try {
            TossPaymentVerifyResponse response = tossPaymentsClient.getPayment(pgPaymentKey);

            // 결제 상태 확인
            if (!response.isDone()) {
                throw new PaymentVerificationException(
                        PaymentErrorCode.PG_PAYMENT_NOT_APPROVED,
                        "결제가 완료되지 않았습니다. 상태: " + response.getStatus()
                );
            }

            // 취소/실패 확인
            if (response.isCanceled()) {
                throw new PaymentVerificationException(
                        PaymentErrorCode.PAYMENT_ALREADY_PROCESSED,
                        "이미 취소된 결제입니다."
                );
            }

            if (response.isFailed()) {
                throw new PaymentVerificationException(
                        PaymentErrorCode.PAYMENT_VERIFICATION_FAILED,
                        "실패한 결제입니다."
                );
            }

            return response;

        } catch (PaymentVerificationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Toss Payments 조회 실패 - pgPaymentKey: {}", pgPaymentKey, e);
            throw new PgException(
                    PaymentErrorCode.PG_CONNECTION_FAILED,
                    "PG사 통신 중 오류가 발생했습니다: " + e.getMessage(),
                    e
            );
        }
    }

    // ===== 결제 취소 (보상 트랜잭션) =====

    /**
     * 결제 취소 (전액 환불)
     * - Order Service의 보상 트랜잭션에서 호출
     * - PG사에 취소 요청
     * - 성공 시: PaymentRefundedEvent 발행
     * - 실패 시: PaymentRefundFailedEvent 발행
     */
    @Transactional
    public Payment cancelPayment(
            String paymentId,
            String orderId,
            String cancelReason) {

        log.info("결제 취소 시작 - paymentId: {}, reason: {}", paymentId, cancelReason);

        // 1. Payment 조회
        Payment payment = findById(paymentId);

        // 2. 주문 ID 검증
        if (!payment.getOrderId().equals(orderId)) {
            throw new PaymentException(
                    PaymentErrorCode.PAYMENT_NOT_FOUND,
                    "결제 정보가 주문과 일치하지 않습니다."
            );
        }

        // 3. 전액 환불 시작
        BigDecimal refundAmount = payment.getAmount();
        payment.startRefund(refundAmount, cancelReason);

        try {
            // 4. PG사 취소 요청
            TossCancelRequest cancelRequest = TossCancelRequest.fullCancel(cancelReason);
            TossCancelResponse cancelResponse = tossPaymentsClient.cancelPayment(
                    payment.getPgInfo().getPgPaymentKey(),
                    cancelRequest
            );

            // 5. 환불 완료
            String pgRefundId = cancelResponse.getCancels().get(0).getTransactionKey();
            payment.completeRefund(refundAmount, pgRefundId);

            // 6. 저장
            Payment savedPayment = paymentRepository.save(payment);

            log.info("결제 취소 완료 - paymentId: {}, refundAmount: {}",
                    savedPayment.getIdValue(), refundAmount);

            // 7. 성공 이벤트 발행
            PaymentRefundedEventData eventData =
                    PaymentRefundedEventData.from(savedPayment, refundAmount, cancelReason);
            eventPublisher.publishPaymentRefunded(eventData);

            return savedPayment;

        } catch (Exception e) {
            // 환불 실패
            payment.failRefund(e.getMessage());
            Payment savedPayment = paymentRepository.save(payment);

            log.error("결제 취소 실패 - paymentId: {}, error: {}",
                    savedPayment.getIdValue(), e.getMessage(), e);

            // 실패 이벤트 발행
            PaymentRefundFailedEventData eventData =
                    PaymentRefundFailedEventData.from(savedPayment, e.getMessage());
            eventPublisher.publishPaymentRefundFailed(eventData);

            throw new RefundException(
                    PaymentErrorCode.REFUND_PROCESSING_FAILED,
                    "결제 취소 중 오류가 발생했습니다: " + e.getMessage(),
                    e
            );
        }
    }

    // ===== 조회 =====

    /**
     * Payment ID로 조회
     */
    @Transactional(readOnly = true)
    public Payment findById(String paymentId) {
        PaymentId id = PaymentId.from(paymentId);
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException(
                        PaymentErrorCode.PAYMENT_NOT_FOUND,
                        "결제를 찾을 수 없습니다: " + paymentId
                ));
    }

    /**
     * Order ID로 조회
     */
    @Transactional(readOnly = true)
    public Payment findByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException(
                        PaymentErrorCode.PAYMENT_NOT_FOUND,
                        "주문에 대한 결제를 찾을 수 없습니다: " + orderId
                ));
    }

    /**
     * PG Payment ID로 조회
     */
    @Transactional(readOnly = true)
    public Optional<Payment> findByPgPaymentId(String pgPaymentId) {
        return paymentRepository.findByPgPaymentId(pgPaymentId);
    }

    // ===== 상태 확인 =====

    /**
     * 결제 검증 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isVerified(String paymentId) {
        Payment payment = findById(paymentId);
        return payment.isVerified();
    }

    /**
     * 환불 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isRefundable(String paymentId) {
        Payment payment = findById(paymentId);
        return payment.isRefundable();
    }
}
