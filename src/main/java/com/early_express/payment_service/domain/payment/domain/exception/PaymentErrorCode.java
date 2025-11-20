package com.early_express.payment_service.domain.payment.domain.exception;

import com.early_express.payment_service.global.presentation.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Payment Service 전용 에러 코드
 *
 * 분류:
 * - PAYMENT_0xx: 결제 관련 에러
 * - PG_1xx: PG사 연동 관련 에러
 * - REFUND_2xx: 환불 관련 에러
 * - VERIFICATION_3xx: 결제 검증 관련 에러
 */
@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    // ===== 결제 관련 에러 (PAYMENT_0xx) =====
    /**
     * 결제를 찾을 수 없음
     */
    PAYMENT_NOT_FOUND("PAYMENT_001", "결제 정보를 찾을 수 없습니다.", 404),
    PAYMENT_CREATION_FAILED("PAYMENT_002", "결제 생성에 실패했습니다.", 500),
    INVALID_PAYMENT_STATUS("PAYMENT_003", "유효하지 않은 결제 상태입니다.", 400),
    PAYMENT_ALREADY_PROCESSED("PAYMENT_004", "이미 처리된 결제입니다.", 409),
    INVALID_PAYMENT_AMOUNT("PAYMENT_005", "결제 금액이 유효하지 않습니다.", 400),
    PAYMENT_CANNOT_BE_CANCELLED("PAYMENT_006", "취소할 수 없는 결제입니다.", 422),
    PAYMENT_ACCESS_DENIED("PAYMENT_007", "해당 결제에 대한 권한이 없습니다.", 403),
    UNSUPPORTED_PAYMENT_METHOD("PAYMENT_008", "지원하지 않는 결제 방식입니다.", 400),
    INVALID_PAYMENT_KEY("PAYMENT_009", "올바르지 않은 결제 키입니다.", 400),

    // ===== PG 연동 관련 에러 (PG_1xx) =====
    PG_CONNECTION_FAILED("PG_101", "PG사 연동에 실패했습니다.", 502),
    INVALID_PG_PAYMENT_ID("PG_102", "유효하지 않은 PG 결제 ID입니다.", 400),
    PG_RESPONSE_PARSE_ERROR("PG_103", "PG사 응답 파싱에 실패했습니다.", 500),
    PG_APPROVAL_FAILED("PG_104", "PG 결제 승인에 실패했습니다.", 502),
    PG_CANCELLATION_FAILED("PG_105", "PG 결제 취소에 실패했습니다.", 502),
    PG_TIMEOUT("PG_106", "PG사 응답 시간이 초과되었습니다.", 504),
    UNSUPPORTED_PG_PROVIDER("PG_107", "지원하지 않는 PG사입니다.", 400),
    PG_AUTHENTICATION_FAILED("PG_108", "PG사 인증에 실패했습니다.", 401),
    PG_AUTH_FAILED("PG_109", "PG사 인증에 실패했습니다.", 401),
    PG_SYSTEM_ERROR("PG_110", "PG사 시스템 오류가 발생했습니다.", 500),
    PG_VERIFICATION_FAILED("PG_111", "PG사 결제 검증에 실패했습니다.", 400),

    // ===== 환불 관련 에러 (REFUND_2xx) =====
    REFUND_CREATION_FAILED("REFUND_201", "환불 생성에 실패했습니다.", 500),
    INVALID_REFUND_AMOUNT("REFUND_202", "환불 금액이 유효하지 않습니다.", 400),
    REFUND_AMOUNT_EXCEEDS_PAYMENT("REFUND_203", "환불 금액이 결제 금액을 초과합니다.", 400),
    REFUND_NOT_ALLOWED("REFUND_204", "환불이 불가능한 상태입니다.", 422),
    ALREADY_REFUNDED("REFUND_205", "이미 환불된 결제입니다.", 409),
    PARTIAL_REFUND_NOT_ALLOWED("REFUND_206", "부분 환불이 지원되지 않습니다.", 422),
    REFUND_PROCESSING_FAILED("REFUND_207", "환불 처리에 실패했습니다.", 500),
    INVALID_CANCEL_REQUEST("REFUND_208", "올바르지 않은 취소 요청입니다.", 400),
    REFUND_FAILED("REFUND_209", "결제 취소에 실패했습니다.", 500),

    // ===== 결제 검증 관련 에러 (VERIFICATION_3xx) =====
    PAYMENT_VERIFICATION_FAILED("VERIFICATION_301", "결제 검증에 실패했습니다.", 400),
    AMOUNT_MISMATCH("VERIFICATION_302", "결제 금액이 일치하지 않습니다.", 400),
    PAYMENT_EXPIRED("VERIFICATION_303", "만료된 결제입니다.", 400),
    INVALID_PAYMENT("VERIFICATION_304", "유효하지 않은 결제입니다.", 400),
    PAYMENT_APPROVAL_TIMEOUT("VERIFICATION_305", "결제 승인 대기 시간이 초과되었습니다.", 408),
    PG_PAYMENT_NOT_APPROVED("VERIFICATION_306", "PG 결제가 승인되지 않았습니다.", 400),
    PAYER_INFO_MISMATCH("VERIFICATION_307", "결제자 정보가 일치하지 않습니다.", 400),
    DUPLICATE_VERIFICATION_REQUEST("VERIFICATION_308", "이미 검증된 결제입니다.", 409);

    private final String code;
    private final String message;
    private final int status;
}