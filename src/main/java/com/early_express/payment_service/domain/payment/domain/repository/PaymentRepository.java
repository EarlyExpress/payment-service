package com.early_express.payment_service.domain.payment.domain.repository;

import com.early_express.payment_service.domain.payment.domain.model.Payment;
import com.early_express.payment_service.domain.payment.domain.model.PaymentStatus;
import com.early_express.payment_service.domain.payment.domain.model.vo.PaymentId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment Repository 인터페이스
 * 도메인 레이어의 리포지토리 계약
 */
public interface PaymentRepository {

    /**
     * 결제 저장 (생성 또는 수정)
     *
     * @param payment 저장할 결제 도메인 객체
     * @return 저장된 결제 도메인 객체
     */
    Payment save(Payment payment);

    /**
     * ID로 결제 조회 (삭제되지 않은 것만)
     *
     * @param paymentId 결제 ID
     * @return 결제 Optional
     */
    Optional<Payment> findById(PaymentId paymentId);

    /**
     * 주문 ID로 결제 조회
     *
     * @param orderId 주문 ID
     * @return 결제 Optional
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * PG 결제 ID로 결제 조회
     *
     * @param pgPaymentId PG 결제 ID
     * @return 결제 Optional
     */
    Optional<Payment> findByPgPaymentId(String pgPaymentId);

    /**
     * PG 결제 ID 존재 여부 확인
     *
     * @param pgPaymentId PG 결제 ID
     * @return 존재 여부
     */
    boolean existsByPgPaymentId(String pgPaymentId);

    /**
     * 지불자 회사 ID로 결제 목록 조회
     *
     * @param payerCompanyId 지불자 회사 ID
     * @return 결제 목록 (생성일시 내림차순)
     */
    List<Payment> findByPayerCompanyId(String payerCompanyId);

    /**
     * 상태별 결제 목록 조회
     *
     * @param status 결제 상태
     * @return 결제 목록
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * 결제 삭제 (소프트 삭제)
     *
     * @param payment 삭제할 결제
     * @param deletedBy 삭제자 ID
     */
    void delete(Payment payment, String deletedBy);

    /**
     * 결제 검색 (동적 쿼리)
     *
     * @param companyId 회사 ID (지불자 또는 수취인)
     * @param status 결제 상태
     * @param pgProvider PG 제공자
     * @param minAmount 최소 금액
     * @param maxAmount 최대 금액
     * @param startDate 시작일
     * @param endDate 종료일
     * @param pageable 페이징 정보
     * @return 페이징된 결제 목록
     */
    Page<Payment> searchPayments(
            String companyId,
            PaymentStatus status,
            String pgProvider,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    /**
     * 검증 실패한 결제 조회
     *
     * @return 검증 실패한 결제 목록
     */
    List<Payment> findVerificationFailedPayments();

    /**
     * 환불 가능한 결제 조회
     *
     * @param companyId 회사 ID
     * @return 환불 가능한 결제 목록
     */
    List<Payment> findRefundablePayments(String companyId);

    /**
     * 관리자용: ID로 결제 조회 (삭제된 것 포함)
     *
     * @param paymentId 결제 ID
     * @return 결제 Optional
     */
    Optional<Payment> findByIdIncludingDeleted(PaymentId paymentId);

    /**
     * 관리자용: 전체 결제 검색 (삭제된 것 포함)
     *
     * @param companyId 회사 ID
     * @param status 결제 상태
     * @param isDeleted 삭제 여부
     * @param startDate 시작일
     * @param endDate 종료일
     * @param pageable 페이징 정보
     * @return 페이징된 결제 목록
     */
    Page<Payment> searchAllPaymentsIncludingDeleted(
            String companyId,
            PaymentStatus status,
            Boolean isDeleted,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
}