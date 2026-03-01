package org.inventoryservice.Job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.inventoryservice.DAO.Inventory;
import org.inventoryservice.Repository.InventoryRepo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryReconciliationJob {

    private final InventoryRepo inventoryRepo;
    private final StringRedisTemplate redisTemplate;
    private static final String STOCK_KEY_PREFIX = "stock:";

    /**
     * 每 30 分钟执行一次对账 (时间可按需调整)
     * 实际生产中，建议在秒杀活动结束后的低峰期执行
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void reconcileStock() {
        log.info(">>> [Reconciliation] Starting stock reconciliation...");

        List<Inventory> allInventories = inventoryRepo.findAll();

        for (Inventory inventory : allInventories) {
            Long productId = inventory.getProductId();
            String key = STOCK_KEY_PREFIX + productId;

            // 1. 获取 Redis 中的当前库存
            String redisStockStr = redisTemplate.opsForValue().get(key);
            if (redisStockStr == null) continue; // 未预热的跳过

            int redisStock = Integer.parseInt(redisStockStr);
            int dbStock = inventory.getStock();

            // 2. 核心比对
            // 注意：由于存在异步落库延迟，DB 库存通常会比 Redis “多”
            // 但如果差异过大，或者在没有进行中的订单时依然不一致，就需要校准
            if (redisStock != dbStock) {
                // 方案 A：以 DB 为准强制覆盖 Redis (适合活动结束后)
                // 方案 B：记录异常日志，人工介入 (适合活动进行中)
                log.warn(">>> [Inconsistency Found] Product: {}, Redis: {}, DB: {}",
                        productId, redisStock, dbStock);

                // 采取“平滑补偿”：如果发现 Redis 数据由于某种原因异常丢失了，手动修复它
                //redisTemplate.opsForValue().set(key, String.valueOf(dbStock));
            }
        }

        log.info(">>> [Reconciliation] Stock reconciliation finished.");
    }
}
