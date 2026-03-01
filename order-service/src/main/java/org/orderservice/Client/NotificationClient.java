package org.orderservice.Client;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonlib.DTO.OrderEvent;
import org.commonlib.Exception.BizException;
import org.commonlib.Exception.ErrorCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Async("notifyExecutor")
    @Retry(name="notificationService", fallbackMethod = "sendFallBack")
    public void notifyOrderCreated(OrderEvent orderEvent) {
       // System.out.println(">>> [Retry Check] Time: " + java.time.LocalTime.now());
        String url = "http://notification-service/notifications/order-created";
        log.info("Asynchronously called notification service, orderID: {}", orderEvent.getOrderId());
        restTemplate.postForObject(url, orderEvent, Void.class);
    }
    public void sendFallBack(OrderEvent orderEvent, Throwable e) {
        log.error("Fallback logic trigerred by notification-service retry. Reason:{}", e.getMessage());
        throw new BizException(ErrorCode.NOTIFICATION_SERVICE_UNAVAILABLE);
    }
}
