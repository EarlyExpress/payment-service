package com.early_express.payment_service.domain.payment.infrastructure.client.toss;

import com.early_express.payment_service.domain.payment.domain.exception.PaymentVerificationException;
import com.early_express.payment_service.domain.payment.domain.exception.RefundException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * TossPaymentsErrorDecoder 단위 테스트
 * HTTP 에러 코드를 도메인 예외로 변환하는 로직 검증
 */
@DisplayName("TossPaymentsErrorDecoder 테스트")
class TossPaymentsErrorDecoderTest {

    private TossPaymentsErrorDecoder errorDecoder;

    @BeforeEach
    void setUp() {
        errorDecoder = new TossPaymentsErrorDecoder();
    }

    // ===== getPayment 메서드 에러 테스트 =====

    @Test
    @DisplayName("getPayment - 400 Bad Request 시 PaymentVerificationException 발생")
    void getPayment_BadRequest() {
        // given
        Response response = createResponse(400, "getPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#getPayment(String)", response);

        // then
        assertThat(exception).isInstanceOf(PaymentVerificationException.class);
        assertThat(exception.getMessage()).contains("올바르지 않은 결제 키");
    }

    @Test
    @DisplayName("getPayment - 401 Unauthorized 시 PaymentVerificationException 발생")
    void getPayment_Unauthorized() {
        // given
        Response response = createResponse(401, "getPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#getPayment(String)", response);

        // then
        assertThat(exception).isInstanceOf(PaymentVerificationException.class);
        assertThat(exception.getMessage()).contains("인증에 실패");
    }

    @Test
    @DisplayName("getPayment - 404 Not Found 시 PaymentVerificationException 발생")
    void getPayment_NotFound() {
        // given
        Response response = createResponse(404, "getPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#getPayment(String)", response);

        // then
        assertThat(exception).isInstanceOf(PaymentVerificationException.class);
        assertThat(exception.getMessage()).contains("찾을 수 없습니다");
    }

    @Test
    @DisplayName("getPayment - 500 Internal Server Error 시 PaymentVerificationException 발생")
    void getPayment_ServerError() {
        // given
        Response response = createResponse(500, "getPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#getPayment(String)", response);

        // then
        assertThat(exception).isInstanceOf(PaymentVerificationException.class);
        assertThat(exception.getMessage()).contains("시스템 오류");
    }

    @Test
    @DisplayName("getPayment - 기타 에러 시 PaymentVerificationException 발생")
    void getPayment_UnknownError() {
        // given
        Response response = createResponse(503, "getPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#getPayment(String)", response);

        // then
        assertThat(exception).isInstanceOf(PaymentVerificationException.class);
        assertThat(exception.getMessage()).contains("검증에 실패");
    }

    // ===== cancelPayment 메서드 에러 테스트 =====

    @Test
    @DisplayName("cancelPayment - 400 Bad Request 시 RefundException 발생")
    void cancelPayment_BadRequest() {
        // given
        Response response = createResponse(400, "cancelPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#cancelPayment(String,TossCancelRequest)", response);

        // then
        assertThat(exception).isInstanceOf(RefundException.class);
        assertThat(exception.getMessage()).contains("올바르지 않은 취소 요청");
    }

    @Test
    @DisplayName("cancelPayment - 401 Unauthorized 시 RefundException 발생")
    void cancelPayment_Unauthorized() {
        // given
        Response response = createResponse(401, "cancelPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#cancelPayment(String,TossCancelRequest)", response);

        // then
        assertThat(exception).isInstanceOf(RefundException.class);
        assertThat(exception.getMessage()).contains("인증에 실패");
    }

    @Test
    @DisplayName("cancelPayment - 404 Not Found 시 RefundException 발생")
    void cancelPayment_NotFound() {
        // given
        Response response = createResponse(404, "cancelPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#cancelPayment(String,TossCancelRequest)", response);

        // then
        assertThat(exception).isInstanceOf(RefundException.class);
        assertThat(exception.getMessage()).contains("찾을 수 없습니다");
    }

    @Test
    @DisplayName("cancelPayment - 409 Conflict 시 RefundException 발생")
    void cancelPayment_Conflict() {
        // given
        Response response = createResponse(409, "cancelPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#cancelPayment(String,TossCancelRequest)", response);

        // then
        assertThat(exception).isInstanceOf(RefundException.class);
        assertThat(exception.getMessage()).contains("취소할 수 없는");
    }

    @Test
    @DisplayName("cancelPayment - 500 Internal Server Error 시 RefundException 발생")
    void cancelPayment_ServerError() {
        // given
        Response response = createResponse(500, "cancelPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#cancelPayment(String,TossCancelRequest)", response);

        // then
        assertThat(exception).isInstanceOf(RefundException.class);
        assertThat(exception.getMessage()).contains("시스템 오류");
    }

    @Test
    @DisplayName("cancelPayment - 기타 에러 시 RefundException 발생")
    void cancelPayment_UnknownError() {
        // given
        Response response = createResponse(503, "cancelPayment");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#cancelPayment(String,TossCancelRequest)", response);

        // then
        assertThat(exception).isInstanceOf(RefundException.class);
        assertThat(exception.getMessage()).contains("취소에 실패");
    }

    // ===== 기타 메서드 에러 테스트 =====

    @Test
    @DisplayName("알 수 없는 메서드의 에러는 기본 ErrorDecoder로 처리")
    void unknownMethod_UsesDefaultDecoder() {
        // given
        Response response = createResponse(404, "unknownMethod");

        // when
        Exception exception = errorDecoder.decode("TossPaymentsClient#unknownMethod()", response);

        // then
        assertThat(exception).isInstanceOf(feign.FeignException.class);
    }

    // ===== Helper Methods =====

    /**
     * 테스트용 Response 객체 생성
     */
    private Response createResponse(int status, String methodKey) {
        return Response.builder()
                .status(status)
                .reason("Test Reason")
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "/test",
                        Collections.emptyMap(),
                        null,
                        StandardCharsets.UTF_8,
                        null
                ))
                .headers(Collections.emptyMap())
                .body("{\"error\":\"test error\"}", StandardCharsets.UTF_8)
                .build();
    }
}