package org.inventoryservice.Service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonlib.Exception.BizException;
import org.commonlib.Exception.ErrorCode;
import org.inventoryservice.DAO.Inventory;
import org.inventoryservice.DAO.InventoryLog;
import org.inventoryservice.Repository.InventoryLogRepo;
import org.inventoryservice.Repository.InventoryRepo;
import org.inventoryservice.Service.InventoryService;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepo inventoryRepo;
    private final StringRedisTemplate redisTemplate;
    private static final String STOCK_KEY_PREFIX = "stock:";
    private final ApplicationContext applicationContext;
    private final InventoryLogRepo inventoryLogRepo;

    // write-behind
    @Override
    public void warmUp(Long productId){
        Inventory inventory = inventoryRepo.findByProductId(productId).orElseThrow(() -> new BizException(ErrorCode.INVENTORY_NOT_FOUND));
        redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + productId, String.valueOf(inventory.getStock()));
        log.info("Stock warmed up for product {}: {}", productId, inventory.getStock());
    }

    // redis deduct + async db
    @Override
    public void cacheDeduct(Long productId, int count) {
        String key = STOCK_KEY_PREFIX + productId;
        Long remaining = executeLua(key, count);
        if (remaining == null){     // lazy load + warm up lock
            String lockKey = "lock:warmup:" + productId;
            String requestId = UUID.randomUUID().toString();
            Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, requestId, Duration.ofSeconds(5));
            if (Boolean.TRUE.equals(isLocked)) {
                try{
                    remaining = executeLua(key, count); // double check
                    if (remaining == null){
                        log.info("Distributed lock acquired. Warming up DB for: {}", productId);
                        warmUp(productId);
                        remaining = executeLua(key, count);
                        if (remaining < 0) throw new BizException(ErrorCode.INVENTORY_NOT_ENOUGH);
                    }
                } finally {
                    String script = """
                        if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) 
                        else 
                            return 0 
                        end
                        """;
                    redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList(lockKey), requestId);
                }
            }
        }


        if (remaining != null && remaining < 0) {
            log.warn("Inventory not enough in Redis for product {}: {}", productId, count);
            throw new BizException(ErrorCode.INVENTORY_NOT_ENOUGH);
        }

        log.info("Redis deducted success. Product: {}, Remaining: {}", productId, remaining);
    }

    @Override
    public void dbDeduct(Long productId, int count, String orderToken) {
        InventoryServiceImpl proxy = (InventoryServiceImpl) applicationContext.getBean("inventoryServiceImpl");
        proxy.updateDb(productId, count, orderToken);
    }

    private Long executeLua(String key, int count){
        String luaScript = """
                local stock = redis.call('get', KEYS[1])
                if (not stock) then 
                    return nil
                end 
                if (tonumber(stock) < tonumber(ARGV[1])) then 
                    return -1
                else
                    return redis.call('decrby', KEYS[1], ARGV[1])
                end
                """;
        Long remaining = redisTemplate.execute(                                       // cluster fail
                new DefaultRedisScript<>(luaScript, Long.class),
                Collections.singletonList(key),
                String.valueOf(count)
        );
        return remaining;
        HttpStatus.CONFLICT
    }

   // @Async("inventoryExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void updateDb(Long productId, int count, String orderToken) {
        log.info("Starting DB deduct for token: {}", orderToken);
        // avoid suspension
        try {
            inventoryLogRepo.insertLogRaw(orderToken, productId, count, 1);
        } catch (Exception e) {
            log.warn("Inventory Log already      exists for token: {}, skipping DB deduct.", orderToken, e);
            return;
        }

        int rows = inventoryRepo.atomicDeduct(productId, count);

        if (rows == 0) {
            log.error("DB Update Failed! Product: {}, Token: {}", productId, orderToken);
            throw new BizException(ErrorCode.INVENTORY_NOT_ENOUGH);
        }
    }

    @Override
    public Inventory getByProductId(Long productId) {
        return inventoryRepo.findByProductId(productId)
                .orElseThrow(()-> new BizException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dbCompensate(Long productId, int count, String orderToken) {
        log.info("Starting DB compensate for token: {}", orderToken);

        int affectedRows = inventoryLogRepo.updateStatusToCompensate(orderToken);

        if (affectedRows > 0) {
            inventoryRepo.increaseStock(productId, count);
            log.info("DB Stock compensated successfully. Token: {}", orderToken);
        } else {
            if (!inventoryLogRepo.existsById(orderToken)) {
                log.warn("Empty Compensation! No deduct record for token: {}. Placeholder inserted.", orderToken);
                inventoryLogRepo.insertLogRaw(orderToken, productId, count, 2);
            } else {
                log.info("Duplicate Compensation. Token: {} already handled.", orderToken);
            }
        }
    }

    @Override
    public void cacheCompensate(Long productId, int count, String orderToken) {
        String compensateKey = "inventory:compensate:" + orderToken;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(compensateKey))) {
            log.warn("Already compensated for token: {}", orderToken);
            return;
        }

        // redis compensate
        try {
            String key = STOCK_KEY_PREFIX + productId;
            redisTemplate.opsForValue().increment(key, count);

            redisTemplate.opsForValue().set(compensateKey, "true", Duration.ofHours(24));

            log.info("Stock compensated successfully. Token: {}", orderToken);

        } catch (Exception e) {
            log.error("Compensate failed for token: {}", orderToken);
            throw e; // upstream retry
        }

    }
}
