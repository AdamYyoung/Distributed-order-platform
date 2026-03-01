package org.orderservice.Client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonlib.Exception.BizException;
import org.commonlib.Exception.ErrorCode;
import org.orderservice.DTO.InventoryResponse;
import org.orderservice.DTO.ProductDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final RestTemplate restTemplate;

    @CircuitBreaker(name="inventoryService", fallbackMethod = "circuitBreakerFallBack")
    @RateLimiter(name="inventoryApi", fallbackMethod = "rateLimitFallBack")
    public void cacheDeduct(Long productId, int count, String orderToken){
        String url = "http://inventory-service/inventory/cacheDeduct";
        Map<String, Object> req = Map.of("productId", productId, "count", count, "orderToken", orderToken);
        InventoryResponse resp = restTemplate.postForObject(url, req, InventoryResponse.class);
        if (resp == null || resp.getCode() != 200){
            throw new BizException(ErrorCode.INVENTORY_NOT_ENOUGH);
        }
    }

    @CircuitBreaker(name="inventoryService", fallbackMethod = "circuitBreakerFallBack")
    @RateLimiter(name="inventoryApi", fallbackMethod = "rateLimitFallBack")
    public void dbDeduct(Long productId, int count, String orderToken){
        String url = "http://inventory-service/inventory/dbDeduct";
        Map<String, Object> req = Map.of("productId", productId, "count", count, "orderToken", orderToken);
        InventoryResponse resp = restTemplate.postForObject(url, req, InventoryResponse.class);
        if (resp == null || resp.getCode() != 200){
            throw new BizException(ErrorCode.INVENTORY_NOT_ENOUGH);
        }
    }

    public void cacheCompensate(Long productId, int count, String orderToken){
        String url = "http://inventory-service/inventory/cacheCompensate";
        Map<String, Object> req = Map.of("productId", productId, "count", count, "orderToken", orderToken);
        InventoryResponse resp = restTemplate.postForObject(url, req, InventoryResponse.class);
        if (resp == null || resp.getCode() != 200){
            throw new BizException(ErrorCode.INVENTORY_SERVICE_UNAVAILABLE);
        }
    }

    public void dbCompensate(Long productId, int count, String orderToken){
        String url = "http://inventory-service/inventory/dbCompensate";
        Map<String, Object> req = Map.of("productId", productId, "count", count, "orderToken", orderToken);
        InventoryResponse resp = restTemplate.postForObject(url, req, InventoryResponse.class);
        if (resp == null || resp.getCode() != 200){
            throw new BizException(ErrorCode.INVENTORY_SERVICE_UNAVAILABLE);
        }
    }

    public void circuitBreakerFallBack(Long productId, int count, String orderToken, Throwable e) {
        log.error("Fallback logic trigerred by inventory-service circuit breaker. Reason:{}", e.getMessage());
        throw new BizException(ErrorCode.INVENTORY_SERVICE_UNAVAILABLE);
    }
    public void rateLimitFallBack(Long productId, int count, String orderToken, Throwable e) {
        log.error("Fallback logic trigerred by inventory-service rate limiter. Reason:{}", e.getMessage());
        throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
    }

}
