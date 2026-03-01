package org.orderservice.Client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonlib.Exception.BizException;
import org.commonlib.Exception.ErrorCode;
import org.commonlib.Response.ApiResponse;
import org.orderservice.DTO.ProductDTO;
import org.orderservice.DTO.ProductResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClient {
    private final RestTemplate restTemplate;

    @CircuitBreaker(name="productService", fallbackMethod = "circuitBreakerFallBack")
    @RateLimiter(name="productApi", fallbackMethod = "rateLimitFallBack")
    public ProductDTO getProduct(Long productId) {
        String url = "http://product-service/product/"+productId;
        ProductResponse resp = restTemplate.getForObject(url, ProductResponse.class); // Type erasure
        if (resp == null || resp.getCode() != 200) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return resp.getData();
    }

    public ProductDTO circuitBreakerFallBack(Long productId, Throwable e) {
        log.error("Fallback logic trigerred by product-service circuit breaker. Reason:{}", e.getMessage());
        throw new BizException(ErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
    }
    public ProductDTO rateLimitFallBack(Long productId, Throwable e) {
        log.error("Fallback logic trigerred by product-service rate limiter. Reason:{}", e.getMessage());
        throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
    }
}
