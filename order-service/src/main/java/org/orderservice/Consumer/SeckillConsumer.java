package org.orderservice.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonlib.DTO.OrderEvent;
import org.orderservice.Client.InventoryClient;
import org.orderservice.Client.NotificationClient;
import org.orderservice.DAO.Order;
import org.orderservice.DTO.SeckillTaskMessage;
import org.orderservice.Proxy.OrderServiceProxy;
import org.orderservice.Service.OrderService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillConsumer {
    private final OrderServiceProxy orderServiceProxy;
    private final InventoryClient inventoryClient;
    private final NotificationClient notificationClient;
    private final StringRedisTemplate redisTemplate;
    @Lazy
    private final OrderService orderService;

    @KafkaListener(topics = "seckill-order-topic", groupId = "seckill-group")
    public void message(SeckillTaskMessage msg){

        log.info("Saga Consumer: Processing task for token: {}", msg.getOrderToken());
        Long orderId = null;
        String resultKey = "seckill:result:" + msg.getOrderToken();

        String currentStatus = redisTemplate.opsForValue().get(resultKey);

        // already been processed, idempotency
        if (currentStatus != null && currentStatus.startsWith("SUCCESS")) {
            log.warn("Message already processed for token: {}", msg.getOrderToken());
            return;
        }

        try {
            log.info("Saga Step 2: local transaction in db, pending");
            Long userId = msg.getUserId();
            Long productId = msg.getProductId();
            int count = msg.getCount();
            BigDecimal price = msg.getPrice();
            Order order = orderServiceProxy.savePendingOrder(userId, productId, count, price);
            orderId = order.getId();

            log.info("Saga step 3: Confirmed deduct in db");
            inventoryClient.dbDeduct(productId, count, msg.getOrderToken());
            log.info("Sage step 4: Update order status success and send notification");

            redisTemplate.opsForValue().set(resultKey, "SUCCESS:" + order.getId(), Duration.ofMinutes(15));
            OrderEvent orderEvent = new OrderEvent(order.getId(), productId, count, order.getAmount());
            notificationClient.notifyOrderCreated(orderEvent);

        } catch (Exception e) {
            log.error("Saga Consumer Error for token: {}. Triggering compensation.", msg.getOrderToken(), e);
            orderService.handleSagaCompensation(
                    orderId, msg.getUserId(), msg.getProductId(), msg.getCount(), resultKey, msg.getOrderToken()
            );
        }
    }
}
