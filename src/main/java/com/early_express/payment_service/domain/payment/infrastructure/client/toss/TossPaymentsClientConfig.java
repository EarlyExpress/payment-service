package com.early_express.payment_service.domain.payment.infrastructure.client.toss;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Toss Payments Client 설정
 */
@Slf4j
public class TossPaymentsClientConfig {

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    /**
     * Basic 인증 헤더 추가
     */
    @Bean
    public RequestInterceptor tossAuthInterceptor() {
        return requestTemplate -> {
            String auth = tossSecretKey + ":";
            String encodedAuth = Base64.getEncoder()
                    .encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            requestTemplate.header("Authorization", "Basic " + encodedAuth);
            requestTemplate.header("Content-Type", "application/json");
        };
    }

    @Bean
    public Logger.Level tossClientLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Request.Options tossRequestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,  // connectTimeout
                30, TimeUnit.SECONDS,  // readTimeout
                true
        );
    }

    @Bean
    public Retryer tossRetryer() {
        return new Retryer.Default(
                200,   // 재시도 간격
                2000,  // 최대 재시도 간격
                3      // 최대 재시도 횟수
        );
    }

    @Bean
    public ErrorDecoder tossErrorDecoder() {
        return new TossPaymentsErrorDecoder();
    }
}
