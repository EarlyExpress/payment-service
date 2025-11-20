package com.early_express.payment_service.domain.payment.infrastructure.persistence.repository;

import com.early_express.payment_service.domain.payment.domain.model.Payment;
import com.early_express.payment_service.domain.payment.domain.model.PaymentStatus;
import com.early_express.payment_service.domain.payment.domain.model.vo.PaymentId;
import com.early_express.payment_service.domain.payment.domain.repository.PaymentRepository;
import com.early_express.payment_service.domain.payment.infrastructure.persistence.entity.PaymentEntity;
import com.early_express.payment_service.domain.payment.infrastructure.persistence.entity.QPaymentEntity;
import com.early_express.payment_service.domain.payment.infrastructure.persistence.jpa.PaymentJpaRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final QPaymentEntity qPayment = QPaymentEntity.paymentEntity;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity;

        // ID가 있으면 기존 엔티티 조회 후 업데이트 (Dirty Checking)
        if (payment.getId() != null) {
            entity = paymentJpaRepository.findById(payment.getIdValue())
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + payment.getIdValue()));
            entity.updateFromDomain(payment);
        } else {
            // ID가 없으면 새로 생성
            entity = PaymentEntity.fromDomain(payment);
        }

        PaymentEntity savedEntity = paymentJpaRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        return paymentJpaRepository.findById(paymentId.getValue())
                .filter(entity -> !entity.isDeleted())
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return paymentJpaRepository.findByOrderIdAndIsDeletedFalse(orderId)
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByPgPaymentId(String pgPaymentId) {
        return paymentJpaRepository.findByPgPaymentIdAndIsDeletedFalse(pgPaymentId)
                .map(PaymentEntity::toDomain);
    }

    @Override
    public boolean existsByPgPaymentId(String pgPaymentId) {
        return paymentJpaRepository.existsByPgPaymentId(pgPaymentId);
    }

    @Override
    public List<Payment> findByPayerCompanyId(String payerCompanyId) {
        return paymentJpaRepository.findByPayerCompanyIdAndIsDeletedFalseOrderByCreatedAtDesc(payerCompanyId)
                .stream()
                .map(PaymentEntity::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return paymentJpaRepository.findByStatusAndIsDeletedFalse(status)
                .stream()
                .map(PaymentEntity::toDomain)
                .toList();
    }

    @Override
    public void delete(Payment payment, String deletedBy) {
        PaymentEntity entity = paymentJpaRepository.findById(payment.getIdValue())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        entity.delete(deletedBy);
        paymentJpaRepository.save(entity);
    }

    // ===== QueryDSL 동적 쿼리 메서드 =====

    /**
     * 결제 검색 (동적 쿼리)
     */
    public Page<Payment> searchPayments(
            String companyId,
            PaymentStatus status,
            String pgProvider,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        List<PaymentEntity> content = queryFactory
                .selectFrom(qPayment)
                .where(
                        companyIdEq(companyId),
                        statusEq(status),
                        pgProviderEq(pgProvider),
                        amountBetween(minAmount, maxAmount),
                        createdAtBetween(startDate, endDate),
                        qPayment.isDeleted.isFalse()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qPayment.createdAt.desc())
                .fetch();

        long total = queryFactory
                .selectFrom(qPayment)
                .where(
                        companyIdEq(companyId),
                        statusEq(status),
                        pgProviderEq(pgProvider),
                        amountBetween(minAmount, maxAmount),
                        createdAtBetween(startDate, endDate),
                        qPayment.isDeleted.isFalse()
                )
                .fetchCount();

        List<Payment> payments = content.stream()
                .map(PaymentEntity::toDomain)
                .toList();

        return new PageImpl<>(payments, pageable, total);
    }

    /**
     * 검증 실패한 결제 조회
     */
    public List<Payment> findVerificationFailedPayments() {
        List<PaymentEntity> entities = queryFactory
                .selectFrom(qPayment)
                .where(
                        qPayment.status.eq(PaymentStatus.VERIFICATION_FAILED),
                        qPayment.isDeleted.isFalse()
                )
                .orderBy(qPayment.createdAt.desc())
                .fetch();

        return entities.stream()
                .map(PaymentEntity::toDomain)
                .toList();
    }

    /**
     * 환불 가능한 결제 조회
     */
    public List<Payment> findRefundablePayments(String companyId) {
        List<PaymentEntity> entities = queryFactory
                .selectFrom(qPayment)
                .where(
                        qPayment.payerCompanyId.eq(companyId),
                        qPayment.status.in(PaymentStatus.VERIFIED, PaymentStatus.PARTIALLY_REFUNDED),
                        qPayment.isDeleted.isFalse()
                )
                .orderBy(qPayment.createdAt.desc())
                .fetch();

        return entities.stream()
                .map(PaymentEntity::toDomain)
                .toList();
    }

    /**
     * 관리자용: 삭제된 결제 포함 조회
     */
    public Optional<Payment> findByIdIncludingDeleted(PaymentId paymentId) {
        return paymentJpaRepository.findById(paymentId.getValue())
                .map(PaymentEntity::toDomain);
    }

    /**
     * 관리자용: 전체 결제 검색 (삭제된 것 포함)
     */
    public Page<Payment> searchAllPaymentsIncludingDeleted(
            String companyId,
            PaymentStatus status,
            Boolean isDeleted,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        List<PaymentEntity> content = queryFactory
                .selectFrom(qPayment)
                .where(
                        companyIdEq(companyId),
                        statusEq(status),
                        isDeletedEq(isDeleted),
                        createdAtBetween(startDate, endDate)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qPayment.createdAt.desc())
                .fetch();

        long total = queryFactory
                .selectFrom(qPayment)
                .where(
                        companyIdEq(companyId),
                        statusEq(status),
                        isDeletedEq(isDeleted),
                        createdAtBetween(startDate, endDate)
                )
                .fetchCount();

        List<Payment> payments = content.stream()
                .map(PaymentEntity::toDomain)
                .toList();

        return new PageImpl<>(payments, pageable, total);
    }

    // ===== QueryDSL 조건 메서드 =====

    private BooleanExpression companyIdEq(String companyId) {
        if (companyId == null) {
            return null;
        }
        return qPayment.payerCompanyId.eq(companyId)
                .or(qPayment.payeeCompanyId.eq(companyId));
    }

    private BooleanExpression statusEq(PaymentStatus status) {
        return status != null ? qPayment.status.eq(status) : null;
    }

    private BooleanExpression pgProviderEq(String pgProvider) {
        return pgProvider != null ? qPayment.pgProvider.eq(pgProvider) : null;
    }

    private BooleanExpression amountBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null) {
            return qPayment.amount.between(minAmount, maxAmount);
        } else if (minAmount != null) {
            return qPayment.amount.goe(minAmount);
        } else if (maxAmount != null) {
            return qPayment.amount.loe(maxAmount);
        }
        return null;
    }

    private BooleanExpression isDeletedEq(Boolean isDeleted) {
        return isDeleted != null ? qPayment.isDeleted.eq(isDeleted) : null;
    }

    private BooleanExpression createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return qPayment.createdAt.between(startDate, endDate);
        } else if (startDate != null) {
            return qPayment.createdAt.goe(startDate);
        } else if (endDate != null) {
            return qPayment.createdAt.loe(endDate);
        }
        return null;
    }

}
