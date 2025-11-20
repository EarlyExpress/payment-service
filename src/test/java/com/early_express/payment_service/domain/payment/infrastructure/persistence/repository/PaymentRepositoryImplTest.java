package com.early_express.payment_service.domain.payment.infrastructure.persistence.repository;

import com.early_express.payment_service.domain.payment.domain.model.*;
import com.early_express.payment_service.domain.payment.domain.model.vo.*;
import com.early_express.payment_service.domain.payment.infrastructure.persistence.entity.PaymentEntity;
import com.early_express.payment_service.domain.payment.infrastructure.persistence.jpa.PaymentJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PaymentRepositoryImpl.class)
class PaymentRepositoryImplTest {

    @Autowired
    private PaymentRepositoryImpl paymentRepository;

    @Autowired
    private PaymentJpaRepository jpaRepository;

    private PgInfo createPgInfo() {
        return PgInfo.of(
                "TOSS",
                "PG_PAYMENT_ID",
                "PG_PAYMENT_KEY",
                "PG_TX_ID",
                LocalDateTime.now()
        );
    }

    private PayerInfo createPayer() {
        return PayerInfo.of("PAYER", "payer name", "payer@test.com", "010-1111-2222");
    }

    private PayeeInfo createPayee() {
        return PayeeInfo.of("PAYEE", "payee name");
    }

    private Payment createVerifiedPayment() {
        Payment p = Payment.create(
                "ORDER-100",
                new BigDecimal("10000"),
                createPgInfo(),
                createPayer(),
                createPayee()
        );
        p.startVerification();
        p.verifySuccess();
        return p;
    }

    @Nested
    @DisplayName("save() 테스트")
    class SaveTest {

        @Test
        @DisplayName("새 Payment 저장 시 ID가 생성되고 조회 가능하다")
        void saveNewPayment() {
            // given
            Payment payment = createVerifiedPayment();

            // when
            Payment saved = paymentRepository.save(payment);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getOrderId()).isEqualTo("ORDER-100");
        }

        @Test
        @DisplayName("기존 Payment 업데이트 시 dirty checking 으로 업데이트된다")
        void updatePayment() {
            // given
            Payment payment = createVerifiedPayment();
            Payment saved = paymentRepository.save(payment);

            // PaymentStatus.PARTIALLY_REFUNDED 로 변경
            saved.startRefund(new BigDecimal("3000"), "부분 환불 요청");

            // when
            Payment updated = paymentRepository.save(saved);

            // then
            Optional<PaymentEntity> entity = jpaRepository.findById(updated.getIdValue());
            assertThat(entity).isPresent();
            assertThat(entity.get().getStatus()).isEqualTo(PaymentStatus.REFUNDING);
        }
    }

    @Nested
    @DisplayName("findById 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("findById 로 저장된 Payment 조회 가능")
        void findPayment() {
            Payment saved = paymentRepository.save(createVerifiedPayment());

            Optional<Payment> result = paymentRepository.findById(saved.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getOrderId()).isEqualTo("ORDER-100");
        }
    }

    @Nested
    @DisplayName("findByStatus 테스트")
    class FindByStatusTest {

        @Test
        @DisplayName("검증 완료 상태인 결제를 조회하면 검색된다")
        void findByStatus() {
            Payment saved = paymentRepository.save(createVerifiedPayment());

            var list = paymentRepository.findByStatus(PaymentStatus.VERIFIED);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getOrderId()).isEqualTo("ORDER-100");
        }
    }
}
