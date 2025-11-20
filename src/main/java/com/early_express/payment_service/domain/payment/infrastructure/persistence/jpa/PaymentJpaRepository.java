package com.early_express.payment_service.domain.payment.infrastructure.persistence.jpa;

import com.early_express.payment_service.domain.payment.domain.model.PaymentStatus;
import com.early_express.payment_service.domain.payment.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Payment JPA Repository
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, String> {

    /**
     * Order ID로 결제 조회
     */
    Optional<PaymentEntity> findByOrderIdAndIsDeletedFalse(String orderId);

    /**
     * PG Payment ID로 결제 조회
     */
    Optional<PaymentEntity> findByPgPaymentIdAndIsDeletedFalse(String pgPaymentId);

    /**
     * PG Payment ID 존재 여부 확인
     */
    boolean existsByPgPaymentId(String pgPaymentId);

    /**
     * 업체별 결제 목록 조회
     */
    List<PaymentEntity> findByPayerCompanyIdAndIsDeletedFalseOrderByCreatedAtDesc(String payerCompanyId);

    /**
     * 상태별 결제 목록 조회
     */
    List<PaymentEntity> findByStatusAndIsDeletedFalse(PaymentStatus status);
}
