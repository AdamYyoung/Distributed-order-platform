package org.orderservice.Service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonlib.DTO.OrderEvent;
import org.commonlib.Exception.BizException;
import org.commonlib.Exception.ErrorCode;
import org.commonlib.Threadlocal.UserContext;
import org.orderservice.Client.InventoryClient;
import org.orderservice.Client.NotificationClient;
import org.orderservice.Client.ProductClient;
import org.orderservice.DAO.Order;
import org.orderservice.DTO.CreateOrderResponse;
import org.orderservice.DTO.ProductDTO;
import org.orderservice.DTO.SeckillResultDTO;
import org.orderservice.DTO.SeckillTaskMessage;
import org.orderservice.Factory.OrderFactory;
import org.orderservice.Proxy.OrderServiceProxy;
import org.orderservice.Repository.OrderRepo;
import org.orderservice.Service.OrderService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final RestTemplate restTemplate;
    private final InventoryClient inventoryClient;
    private final ProductClient productClient;
    private final OrderRepo orderRepo;
    private final NotificationClient notificationClient;

    private final OrderFactory orderFactory;
    private final Executor orderExecutor;
    private final StringRedisTemplate redisTemplate;
    private final Executor productQueryExecutor;
    private final OrderServiceProxy orderServiceProxy;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String SECKILL_TOPIC = "seckill-order-topic";


    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResponse createOrder(Long productId, int count) {
        log.info("1. [Main] Order start. Thread:{}", Thread.currentThread().getName());
        // 1. product info async
        CompletableFuture<ProductDTO> productFuture = CompletableFuture.supplyAsync(()->{
            log.info("2. [Async Query] Fetching product info by Thread:{}", Thread.currentThread().getName());
            ProductDTO productDTO = productClient.getProduct(productId);
            if (productDTO == null) {
                throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            return productDTO;
        }, productQueryExecutor);
        String orderToken = UUID.randomUUID().toString();
        boolean cacheDeducted = false;
        // 2. save
        try{
            log.info("3. Thread:{}", Thread.currentThread().getName());
            //ProductDTO productDTO = productFuture.join();
            ProductDTO productDTO = productFuture.get(5, TimeUnit.SECONDS);

            Long userId = UserContext.get();
            Order order = orderFactory.createOrder(productId, count, productDTO.getPrice(), userId);
            orderRepo.save(order);

            inventoryClient.cacheDeduct(productId, count, orderToken);
            cacheDeducted = true;
            inventoryClient.dbDeduct(productId, count, orderToken);
            order.setStatus("CREATED");

            triggerNotifyAfterCommit(order, productId, count);

            return new CreateOrderResponse(order.getId(), order.getAmount(), order.getStatus(),"");
        } catch (Exception e) {
            log.error("Order creation Failed:{}", e.getMessage());
            if (cacheDeducted) inventoryClient.cacheCompensate(productId, count, orderToken);
            throw (e instanceof BizException) ? (BizException) e : new BizException(ErrorCode.ORDER_FAILED);
        }

    }

    private void triggerNotifyAfterCommit(Order order, Long productId, int count) {
        final Long orderId = order.getId();
        final BigDecimal orderAmount = order.getAmount();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    OrderEvent event = new OrderEvent(orderId, productId, count, orderAmount);
                    CompletableFuture.runAsync(()->{
                        notificationClient.notifyOrderCreated(event); // event should not change!!
                    }, orderExecutor);
                }
            });
        }
    }

    @Override
    public CreateOrderResponse createSecKillOrder(Long productId, int count) {
        boolean inventoryDeducted = false;
        boolean limitChecked = false;
        Long userId = UserContext.get();

        log.info("Stage 1: trace token");
        String orderToken = UUID.randomUUID().toString();
        String resultKey = "seckill:result:" + orderToken;

        try{
            log.info("Stage 2: idempotency check");
            checkSecKillLimits(userId, productId);
            limitChecked = true;

            // TODO: simplify
            log.info("Stage 3: get product info async");
            CompletableFuture<ProductDTO> productFuture = CompletableFuture.supplyAsync(()->{
                ProductDTO productDTO = productClient.getProduct(productId);
                if (productDTO == null || productDTO.getStatus() != 2) {
                    throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                return productDTO;
            }, productQueryExecutor);

            ProductDTO productDTO = productFuture.get(4, TimeUnit.SECONDS);

            log.info("Saga Step 1. deduct inventory in redis ahead");
            inventoryClient.cacheDeduct(productId, count, orderToken);
            inventoryDeducted = true;

            redisTemplate.opsForValue().set(resultKey, "WAIT", Duration.ofMinutes(15));

            SeckillTaskMessage message = new SeckillTaskMessage(
                    userId, productId, count, productDTO.getPrice(), orderToken
            );
            try{
                kafkaTemplate.send(SECKILL_TOPIC, message);
                log.info("Saga task dispatched to Kafka: {}", orderToken);
            } catch (Exception e){
                log.error("Failed to send Kafka message, starting immediate compensation", e);
                throw new BizException(ErrorCode.SECKILL_FAILED);
            }

            return new CreateOrderResponse(null, productDTO.getPrice().multiply(BigDecimal.valueOf(count)), "QUEUEING", orderToken);
        } catch (Exception e){
            if (limitChecked) rollbackSeckillLimit(userId, productId);
            if (inventoryDeducted) handleCompensate(userId, productId, count, orderToken);
            redisTemplate.delete(resultKey);
            log.error("Sec Kill Failed:{}", e.getMessage());
            throw (e instanceof BizException) ? (BizException) e : new BizException(ErrorCode.SECKILL_FAILED);
        }

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSagaCompensation(Long orderId, Long userId, Long productId, int count, String resultKey, String orderToken) {
        String currentStatus = redisTemplate.opsForValue().get(resultKey);
        if ("FAIL".equals(currentStatus)) {
            log.info("Compensation already in progress or finished for token: {}", orderToken);
            return;
        }

        log.info("1. set redis fail");
        redisTemplate.opsForValue().set(resultKey, "FAIL", Duration.ofMinutes(15));
        rollbackSeckillLimit(userId, productId);
        handleCompensate(userId, productId, count, orderToken);
        inventoryClient.dbCompensate(productId, count, orderToken);
        CompletableFuture.runAsync(() -> {
            log.info("2. set order status fail if exist");
            if (orderId != null) {
                try {
                    orderServiceProxy.updateOrderStatus(orderId, "FAIL"); //
                } catch (Exception e) {
                    log.error("Failed to update order status to FAIL for orderId: {}", orderId);
                }
            }

        }, orderExecutor);
    }

    @Override
    public SeckillResultDTO getSecKillResult(String orderToken) {
        String resultKey = "seckill:result:" + orderToken;
        String val = redisTemplate.opsForValue().get(resultKey);

        if (val == null) return new SeckillResultDTO("INVALID", null);
        if ("WAIT".equals(val)) return new SeckillResultDTO("WAIT", null);
        if ("FAIL".equals(val)) return new SeckillResultDTO("FAIL", null);
        if (val.startsWith("SUCCESS")){
            Long orderId = Long.valueOf(val.split(":")[1]);
            return new SeckillResultDTO("SUCCESS", orderId);
        }
        return new SeckillResultDTO("UNKNOWN", null);
    }


    private void checkSecKillLimits(Long userId, Long productId){
        String key = "seckill:limit:" + productId + ":" + userId;
        String requestId = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, requestId, Duration.ofSeconds(1)); // temporarily
        if (Boolean.FALSE.equals(success)){
            throw new BizException(ErrorCode.REPETITIVE_OPERATION);
        }
    }

    private void rollbackSeckillLimit(Long userId, Long productId){
        redisTemplate.delete("seckill:limit:" + productId + ":" + userId);
    }

    private void handleCompensate(Long userId, Long productId, int count, String orderToken) {
        try {
            inventoryClient.cacheCompensate(productId, count, orderToken);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to compensate inventory for product: {}", productId, e);
        }
    }
}

