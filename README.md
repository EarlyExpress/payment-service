# Payment Service

> Early Express 플랫폼의 결제 처리를 담당하는 마이크로서비스

## 📋 개요

Payment Service는 PG사(Toss Payments) 연동을 통한 결제 검증, 환불 처리, 결제 상태 관리를 담당합니다.
DDD(Domain-Driven Design) 아키텍처를 기반으로 설계되었으며, Order Service와의 Saga 패턴을 통해 분산 트랜잭션을 처리합니다.

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| **Framework** | Spring Boot 3.5.7, Spring Cloud 2025.0.0 |
| **Language** | Java 21 |
| **Database** | PostgreSQL + pgvector |
| **ORM** | Spring Data JPA, QueryDSL 5.1.0 |
| **Message Queue** | Apache Kafka (Spring Cloud Stream) |
| **Service Discovery** | Netflix Eureka Client |
| **Config** | Spring Cloud Config |
| **Security** | Spring Security, OAuth 2.0 (Keycloak) |
| **PG 연동** | Toss Payments API |
| **Observability** | Micrometer, Zipkin, Loki, Prometheus |

## 🏗 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        Payment Service                          │
├─────────────────────────────────────────────────────────────────┤
│  Presentation Layer                                             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  PaymentInternalController (Internal API for Order Svc)   │  │
│  └───────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  Application Layer                                              │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  PaymentService (비즈니스 로직 조율)                        │  │
│  └───────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                   │
│  ┌─────────────────┐  ┌─────────────────────────────────────┐  │
│  │ Payment (AR)    │  │ Value Objects                       │  │
│  │ - PaymentStatus │  │ - PaymentId, PaymentAmountInfo      │  │
│  │ - 검증/환불 로직  │  │ - PayerInfo, PayeeInfo, PgInfo      │  │
│  └─────────────────┘  └─────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐    │
│  │ JPA Entity   │  │ Repository   │  │ TossPaymentsClient │    │
│  │ PaymentEntity│  │ Impl         │  │ (PG 연동)          │    │
│  └──────────────┘  └──────────────┘  └────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## 📦 도메인 모델

### Payment (Aggregate Root)

결제의 전체 생명주기를 관리하는 핵심 도메인 모델입니다.

```
Payment
├── PaymentId (식별자)
├── orderId (주문 참조)
├── PaymentAmountInfo (금액 정보)
│   ├── amount (결제 금액)
│   ├── refundedAmount (환불된 금액)
│   └── currency (통화)
├── PgInfo (PG사 정보)
│   ├── pgProvider (TOSS 등)
│   ├── pgPaymentId / pgPaymentKey
│   ├── pgTransactionId
│   └── pgApprovedAt / pgRefundedAt
├── PayerInfo (결제자 정보)
│   ├── payerCompanyId / payerName
│   └── payerEmail / payerPhone
├── PayeeInfo (수취인 정보)
│   └── payeeCompanyId / payeeName
└── PaymentStatus (상태)
```

### 결제 상태 흐름 (PaymentStatus)

```
                    ┌──────────────────┐
                    │     PENDING      │ 검증 대기 중
                    └────────┬─────────┘
                             │ startVerification()
                             ▼
                    ┌──────────────────┐
                    │    VERIFYING     │ 검증 중
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
    │  VERIFIED   │  │VERIFICATION │  │  CANCELLED  │
    │ (검증 완료)  │  │   FAILED    │  │   (취소)    │
    └──────┬──────┘  └─────────────┘  └─────────────┘
           │
           │ startRefund()
           ▼
    ┌─────────────┐
    │  REFUNDING  │ 환불 처리 중
    └──────┬──────┘
           │
    ┌──────┼──────────────┐
    │      │              │
    ▼      ▼              ▼
┌────────┐ ┌───────────┐ ┌────────────┐
│REFUNDED│ │ PARTIALLY │ │REFUND_FAILED│
│(전액)  │ │ REFUNDED  │ │  (실패)     │
└────────┘ └───────────┘ └────────────┘
```

## 🔌 API 엔드포인트

### Internal API (Order Service 전용)

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/v1/payment/internal/all/verify-and-register` | 결제 검증 및 등록 (Saga Step 2) |
| `GET` | `/v1/payment/internal/all/{paymentId}` | Payment ID로 조회 |
| `GET` | `/v1/payment/internal/all/by-order/{orderId}` | Order ID로 조회 |

### 결제 검증 요청 예시

```json
POST /v1/payment/internal/all/verify-and-register
{
  "orderId": "uuid-order-id",
  "pgProvider": "TOSS",
  "pgPaymentId": "pg-payment-id",
  "pgPaymentKey": "pg-payment-key",
  "expectedAmount": 50000,
  "payerCompanyId": "payer-company-uuid",
  "payerName": "홍길동",
  "payerEmail": "hong@example.com",
  "payerPhone": "010-1234-5678",
  "payeeCompanyId": "payee-company-uuid",
  "payeeName": "판매자상호"
}
```

### 응답 예시

```json
{
  "paymentId": "payment-uuid",
  "orderId": "uuid-order-id",
  "status": "VERIFIED",
  "pgTransactionId": "toss-transaction-id",
  "verifiedAmount": 50000,
  "pgApprovedAt": "2025-01-15T10:30:00",
  "verifiedAt": "2025-01-15T10:30:05",
  "message": "결제 검증이 완료되었습니다."
}
```

## ⚙️ 환경 설정

### 필수 환경 변수

```bash
# Application
APP_PORT=4013
APP_NAME=payment-service
APP_PROFILE=dev

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=default_db
DB_USERNAME=postgres
DB_PASSWORD=postgres123!

# Eureka
EUREKA_DEFAULT_ZONE=https://www.pinjun.xyz/eureka1/eureka/,https://www.pinjun.xyz/eureka2/eureka/
EUREKA_INSTANCE_HOSTNAME=192.168.0.42

# Config Server
CONFIG_SERVER_URL=https://www.pinjun.xyz/config

# Kafka
KAFKA_BOOTSTRAP_SERVERS=61.254.69.188:9092,61.254.69.188:9093,61.254.69.188:9094
KAFKA_CONSUMER_GROUP_ID=payment-service-group

# Keycloak (OAuth 2.0)
KEYCLOAK_ISSUER_URI=https://www.pinjun.xyz/keycloak/realms/codefactory
KEYCLOAK_CLIENT_ID=user
KEYCLOAK_CLIENT_SECRET=user-password

# Toss Payments
TOSS=test_sk_P9BRQmyarYYkpPqm5a7prJ07KzLN

# Observability
ZIPKIN_ENABLED=true
ZIPKIN_BASE_URL=https://www.pinjun.xyz/zipkin
LOKI_ENABLED=true
LOKI_URL=https://www.pinjun.xyz/loki/api/v1/push
PROMETHEUS_PUSHGATEWAY_ENABLED=true
PROMETHEUS_PUSHGATEWAY_URL=https://www.pinjun.xyz/prometheus/pushgateway
```

## 🚀 실행 방법

### 로컬 개발 환경

```bash
# 1. 환경 변수 설정
cp .env.example .env
# .env 파일 수정

# 2. Gradle 빌드
./gradlew clean build

# 3. 애플리케이션 실행
./gradlew bootRun

# 또는 JAR 직접 실행
java -jar build/libs/payment-service-0.0.1-SNAPSHOT.jar
```

### Docker 실행

```bash
docker build -t payment-service .
docker run -p 4013:4013 --env-file .env payment-service
```

## 📊 Saga 패턴 연동

Payment Service는 Order Service와 Saga 패턴으로 연동됩니다.

```
Order Service                    Payment Service
     │                                │
     │  1. 주문 생성                   │
     │─────────────────────────────────│
     │                                │
     │  2. POST /verify-and-register  │
     │───────────────────────────────▶│
     │                                │ PG 검증
     │                                │ Payment 생성
     │  3. 검증 결과 반환              │
     │◀───────────────────────────────│
     │                                │
     │  (실패 시) 보상 트랜잭션         │
     │  4. 결제 취소 요청               │
     │───────────────────────────────▶│
     │                                │ PG 환불
     │  5. PaymentRefundedEvent       │
     │◀───────────────────────────────│
```

## 📨 Kafka 이벤트

Payment Service는 **토픽 분리 패턴**을 사용하여 이벤트를 발행/수신합니다.

### 이벤트 흐름도

```
┌─────────────────┐                              ┌─────────────────┐
│  Order Service  │                              │ Payment Service │
└────────┬────────┘                              └────────┬────────┘
         │                                                │
         │  ┌─────────────────────────────────────────┐   │
         │  │       Topic: refund-requested           │   │
         │  └─────────────────────────────────────────┘   │
         │                      │                         │
         │ RefundRequestedEvent │                         │
         │──────────────────────┼────────────────────────▶│
         │                      │                         │
         │                      │    PaymentService.      │
         │                      │    cancelPayment()      │
         │                      │                         │
         │  ┌─────────────────────────────────────────┐   │
         │  │       Topic: payment-refunded           │   │
         │  └─────────────────────────────────────────┘   │
         │                      │                         │
         │◀─────────────────────┼─────────────────────────│
         │  PaymentRefundedEvent│                         │
         │                      │                         │
         │  ┌─────────────────────────────────────────┐   │
         │  │     Topic: payment-refund-failed        │   │
         │  └─────────────────────────────────────────┘   │
         │                      │                         │
         │◀─────────────────────┼─────────────────────────│
         │PaymentRefundFailedEvt│                         │
         │                      │                         │
```

### 수신 이벤트 (Consumer)

| Topic | Event | 설명 | 처리 |
|-------|-------|------|------|
| `refund-requested` | `RefundRequestedEvent` | Order Service에서 환불 요청 | `PaymentService.cancelPayment()` 호출 |

```json
// RefundRequestedEvent 예시
{
  "eventId": "uuid",
  "eventType": "REFUND_REQUESTED",
  "source": "order-service",
  "paymentId": "payment-uuid",
  "orderId": "order-uuid",
  "refundReason": "고객 요청에 의한 취소",
  "requestedAt": "2025-01-15T10:30:00"
}
```

### 발행 이벤트 (Publisher)

| Topic | Event | 설명 | 발행 시점 |
|-------|-------|------|----------|
| `payment-refunded` | `PaymentRefundedEvent` | 환불 성공 | PG 환불 완료 후 |
| `payment-refund-failed` | `PaymentRefundFailedEvent` | 환불 실패 | PG 환불 실패 시 |

```json
// PaymentRefundedEvent 예시
{
  "eventId": "uuid",
  "eventType": "PAYMENT_REFUNDED",
  "source": "payment-service",
  "paymentId": "payment-uuid",
  "orderId": "order-uuid",
  "refundAmount": 50000,
  "totalRefundedAmount": 50000,
  "refundReason": "고객 요청에 의한 취소",
  "pgRefundId": "toss-refund-id",
  "fullRefund": true,
  "refundedAt": "2025-01-15T10:35:00"
}
```

```json
// PaymentRefundFailedEvent 예시
{
  "eventId": "uuid",
  "eventType": "PAYMENT_REFUND_FAILED",
  "source": "payment-service",
  "paymentId": "payment-uuid",
  "orderId": "order-uuid",
  "requestedRefundAmount": 50000,
  "errorMessage": "PG사 통신 오류",
  "failedAt": "2025-01-15T10:35:00"
}
```

### Kafka 설정

```yaml
spring:
  kafka:
    topic:
      refund-requested: refund-requested      # 수신
      payment-refunded: payment-refunded      # 발행
      payment-refund-failed: payment-refund-failed  # 발행
    consumer:
      group-id: payment-service-group
      enable-auto-commit: false  # 수동 ACK
```

## 🔐 보안

- OAuth 2.0 Resource Server (Keycloak 연동)
- Internal API는 서비스 간 통신 전용 (Gateway 미노출)
- PG Secret Key 환경 변수 분리

## 📈 모니터링

| 도구 | 용도 | 엔드포인트 |
|------|------|-----------|
| **Actuator** | 헬스체크/메트릭 | `/actuator/health`, `/actuator/prometheus` |
| **Zipkin** | 분산 추적 | Push to Zipkin Server |
| **Loki** | 로그 수집 | Push via Logback Appender |
| **Prometheus** | 메트릭 수집 | Push to Pushgateway |

## 📁 프로젝트 구조

```
src/main/java/com/early_express/payment_service/
├── domain/payment/
│   ├── application/
│   │   └── service/
│   │       └── PaymentService.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Payment.java (Aggregate Root)
│   │   │   ├── PaymentStatus.java
│   │   │   └── vo/
│   │   │       ├── PaymentId.java
│   │   │       ├── PaymentAmountInfo.java
│   │   │       ├── PayerInfo.java
│   │   │       ├── PayeeInfo.java
│   │   │       └── PgInfo.java
│   │   ├── exception/
│   │   ├── messaging/
│   │   │   ├── PaymentEventPublisher.java (Interface)
│   │   │   ├── PaymentRefundedEventData.java
│   │   │   └── PaymentRefundFailedEventData.java
│   │   └── repository/
│   ├── infrastructure/
│   │   ├── client/toss/
│   │   │   └── TossPaymentsClient.java
│   │   ├── messaging/
│   │   │   ├── order/
│   │   │   │   ├── consumer/
│   │   │   │   │   └── RefundRequestedEventConsumer.java
│   │   │   │   └── event/
│   │   │   │       └── RefundRequestedEvent.java
│   │   │   └── payment/
│   │   │       ├── publisher/
│   │   │       │   └── PaymentEventPublisherImpl.java
│   │   │       └── event/
│   │   │           ├── PaymentRefundedEvent.java
│   │   │           └── PaymentRefundFailedEvent.java
│   │   └── persistence/
│   │       └── entity/
│   │           └── PaymentEntity.java
│   └── presentation/
│       └── internal/
│           └── PaymentInternalController.java
└── global/
    ├── common/
    └── infrastructure/
        └── event/base/
            └── BaseEvent.java
```

