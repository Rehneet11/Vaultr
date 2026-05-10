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
        Boolean isFirstRequest;

        // 1. FAIL-OPEN LOCKING
        try {
            isFirstRequest = redisTemplate.opsForValue()
                    .setIfAbsent(cacheKey, IdempotencyValue.inProgress(), idempotentConfig.ttlMinutes(), TimeUnit.MINUTES);
        } catch (Exception redisEx) {
            log.error("CRITICAL: Redis is DOWN. Failing OPEN and bypassing idempotency for key: {}", cacheKey);
            // Redis is dead, so we just execute the transaction and hope it's not a duplicate
            return joinPoint.proceed();
        }

        // 2. DUPLICATE HANDLING
        if (Boolean.FALSE.equals(isFirstRequest)) {
            return handleDuplicate(cacheKey);
        }

        try {
            // 3. EXECUTE BUSINESS LOGIC
            Object result = joinPoint.proceed();

            // 4. FAIL-SAFE CACHING
            if (result instanceof ResponseEntity<?> responseEntity) {
                try {
                    String serializedBody = objectMapper.writeValueAsString(responseEntity.getBody());
                    IdempotencyValue completedValue = IdempotencyValue.completed(
                            responseEntity.getStatusCode().value(),
                            serializedBody
                    );
                    redisTemplate.opsForValue().set(cacheKey, completedValue, idempotentConfig.ttlMinutes(), TimeUnit.MINUTES);
                } catch (Exception cacheEx) {
                    log.warn("Failed to cache successful response in Redis for key: {}. User still gets success response.", cacheKey);
                }
            }

            return result;

        } catch (Exception e) {
            // 5. DOUBLE-FAULT PROTECTION
            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception deleteEx) {
                log.error("Failed to release idempotency lock due to Redis failure for key: {}", cacheKey);
            }
            throw e; // Always throw the ORIGINAL business exception
        }
    }

    private Object handleDuplicate(String cacheKey) throws Exception {
        Object cachedObj;
        try {
            cachedObj = redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.error("Redis failed while fetching duplicate response for key: {}", cacheKey);
            throw new ConcurrentRequestException("Unable to verify transaction state. Please check your wallet balance.");
        }

        // Your clever workaround for LinkedHashMap mapping - leave this exactly as is!
        IdempotencyValue cachedResponse = objectMapper.convertValue(cachedObj, IdempotencyValue.class);

        if (cachedResponse != null && cachedResponse.isDone()) {
            log.info("Idempotency hit! Returning cached response for key: {}", cacheKey);

            return ResponseEntity
                    .status(cachedResponse.status())
                    .header("X-Idempotency-Hit", "true")
                    .body(objectMapper.readTree(cachedResponse.responseBody()));
        } else {
            log.warn("Concurrent request blocked for key: {}", cacheKey);
            throw new ConcurrentRequestException("Request is currently processing. Please wait.");
        }
    }
}