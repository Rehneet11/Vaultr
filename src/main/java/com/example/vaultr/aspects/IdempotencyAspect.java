package com.example.vaultr.aspects;

import com.example.vaultr.annotations.Idempotent;
import com.example.vaultr.exceptions.ConcurrentRequestException;
import com.example.vaultr.records.IdempotencyValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotentConfig)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotentConfig) throws Throwable {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is strictly required for financial transactions.");
        }

        String cacheKey = idempotentConfig.cachePrefix() + idempotencyKey;

        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(cacheKey, IdempotencyValue.inProgress(), idempotentConfig.ttlMinutes(), TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            return handleDuplicate(cacheKey);
        }

        try {

            Object result = joinPoint.proceed();

            if (result instanceof ResponseEntity<?> responseEntity) {
                String serializedBody = objectMapper.writeValueAsString(responseEntity.getBody());

                IdempotencyValue completedValue = IdempotencyValue.completed(
                        responseEntity.getStatusCode().value(),
                        serializedBody
                );

                redisTemplate.opsForValue().set(cacheKey, completedValue, idempotentConfig.ttlMinutes(), TimeUnit.MINUTES);
            }

            return result;

        } catch (Exception e) {
            redisTemplate.delete(cacheKey);
            throw e;
        }
    }

    private Object handleDuplicate(String cacheKey) throws Exception {
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);

        IdempotencyValue cachedResponse = objectMapper.convertValue(cachedObj, IdempotencyValue.class);

        if (cachedResponse != null && cachedResponse.isDone()) {
            log.info("Idempotency hit! Returning cached response for key: {}", cacheKey);

            return ResponseEntity
                    .status(cachedResponse.status())
                    .header("X-Idempotency-Hit", "true") // Proves to interviewers it came from cache
                    .body(objectMapper.readTree(cachedResponse.responseBody()));
        } else {
            log.warn("Concurrent request blocked for key: {}", cacheKey);
            throw new ConcurrentRequestException("Request is currently processing. Please wait.");
        }
    }
}