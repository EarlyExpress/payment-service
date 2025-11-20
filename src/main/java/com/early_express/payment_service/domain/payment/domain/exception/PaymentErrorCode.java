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

    /**
     * 결제 생성 실패
     */
    PAYMENT_CREATION_FAILED("PAYMENT_002", "결제 생성에 실패했습니다.", 500),

    /**
     * 결제 상태가 유효하지 않음
     */
    INVALID_PAYMENT_STATUS("PAYMENT_003", "유효하지 않은 결제 상태입니다.", 400),

    /**
     * 이미 처리된 결제
     */
    PAYMENT_ALREADY_PROCESSED("PAYMENT_004", "이미 처리된 결제입니다.", 409),

    /**
     * 결제 금액이 0 이하
     */
    INVALID_PAYMENT_AMOUNT("PAYMENT_005", "결제 금액이 유효하지 않습니다.", 400),

    /**
     * 결제 취소 불가
     */
    PAYMENT_CANNOT_BE_CANCELLED("PAYMENT_006", "취소할 수 없는 결제입니다.", 422),

    /**
     * 결제 권한 없음
     */
    PAYMENT_ACCESS_DENIED("PAYMENT_007", "해당 결제에 대한 권한이 없습니다.", 403),

    /**
     * 결제 방식이 지원되지 않음
     */
    UNSUPPORTED_PAYMENT_METHOD("PAYMENT_008", "지원하지 않는 결제 방식입니다.", 400),

    // ===== PG 연동 관련 에러 (PG_1xx) =====
    /**
     * PG사 연동 실패
     */
    PG_CONNECTION_FAILED("PG_101", "PG사 연동에 실패했습니다.", 502),

    /**
     * PG 결제 ID가 유효하지 않음
     */
    INVALID_PG_PAYMENT_ID("PG_102", "유효하지 않은 PG 결제 ID입니다.", 400),

    /**
     * PG 응답 파싱 실패
     */
    PG_RESPONSE_PARSE_ERROR("PG_103", "PG사 응답 파싱에 실패했습니다.", 500),

    /**
     * PG 승인 실패
     */
    PG_APPROVAL_FAILED("PG_104", "PG 결제 승인에 실패했습니다.", 502),

    /**
     * PG 취소 실패
     */
    PG_CANCELLATION_FAILED("PG_105", "PG 결제 취소에 실패했습니다.", 502),

    /**
     * PG 타임아웃
     */
    PG_TIMEOUT("PG_106", "PG사 응답 시간이 초과되었습니다.", 504),

    /**
     * 지원하지 않는 PG사
     */
    UNSUPPORTED_PG_PROVIDER("PG_107", "지원하지 않는 PG사입니다.", 400),

    /**
     * PG 인증 실패
     */
    PG_AUTHENTICATION_FAILED("PG_108", "PG사 인증에 실패했습니다.", 401),

    // ===== 환불 관련 에러 (REFUND_2xx) =====
    /**
     * 환불 생성 실패
     */
    REFUND_CREATION_FAILED("REFUND_201", "환불 생성에 실패했습니다.", 500),

    /**
     * 환불 금액이 유효하지 않음
     */
    INVALID_REFUND_AMOUNT("REFUND_202", "환불 금액이 유효하지 않습니다.", 400),

    /**
     * 환불 금액이 결제 금액 초과
     */
    REFUND_AMOUNT_EXCEEDS_PAYMENT("REFUND_203", "환불 금액이 결제 금액을 초과합니다.", 400),

    /**
     * 환불 불가 상태
     */
    REFUND_NOT_ALLOWED("REFUND_204", "환불이 불가능한 상태입니다.", 422),

    /**
     * 이미 환불된 결제
     */
    ALREADY_REFUNDED("REFUND_205", "이미 환불된 결제입니다.", 409),

    /**
     * 부분 환불 불가
     */
    PARTIAL_REFUND_NOT_ALLOWED("REFUND_206", "부분 환불이 지원되지 않습니다.", 422),

    /**
     * 환불 처리 실패
     */
    REFUND_PROCESSING_FAILED("REFUND_207", "환불 처리에 실패했습니다.", 500),

    // ===== 결제 검증 관련 에러 (VERIFICATION_3xx) =====
    /**
     * 결제 검증 실패
     */
    PAYMENT_VERIFICATION_FAILED("VERIFICATION_301", "결제 검증에 실패했습니다.", 400),

    /**
     * 결제 금액 불일치
     */
    AMOUNT_MISMATCH("VERIFICATION_302", "결제 금액이 일치하지 않습니다.", 400),

    /**
     * 결제 만료
     */
    PAYMENT_EXPIRED("VERIFICATION_303", "만료된 결제입니다.", 400),

    /**
     * 유효하지 않은 결제
     */
    INVALID_PAYMENT("VERIFICATION_304", "유효하지 않은 결제입니다.", 400),

    /**
     * 결제 승인 대기 시간 초과
     */
    PAYMENT_APPROVAL_TIMEOUT("VERIFICATION_305", "결제 승인 대기 시간이 초과되었습니다.", 408),

    /**
     * PG 결제 상태가 승인되지 않음
     */
    PG_PAYMENT_NOT_APPROVED("VERIFICATION_306", "PG 결제가 승인되지 않았습니다.", 400),

    /**
     * 결제자 정보 불일치
     */
    PAYER_INFO_MISMATCH("VERIFICATION_307", "결제자 정보가 일치하지 않습니다.", 400),

    /**
     * 중복 결제 검증 요청
     */
    DUPLICATE_VERIFICATION_REQUEST("VERIFICATION_308", "이미 검증된 결제입니다.", 409);

    private final String code;
    private final String message;
    private final int status;
}