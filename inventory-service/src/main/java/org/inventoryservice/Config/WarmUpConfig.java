package org.inventoryservice.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.inventoryservice.Repository.InventoryRepo;
import org.inventoryservice.Service.InventoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WarmUpConfig implements CommandLineRunner {
    private final InventoryService inventoryService;
    private final InventoryRepo inventoryRepo;
    @Override
    public void run(String... args) {
        log.info(">>> [Cold Start] Starting stock warm-up...");
        inventoryRepo.findAll().forEach(inventory -> {
            try {
                if (inventory.getStatus() != null && inventory.getStatus() == 2)
                    inventoryService.warmUp(inventory.getProductId());
            } catch (Exception e) {
                log.error("Failed to warm up stock for product: {}", inventory.getProductId(), e);
            }
        });
        log.info(">>> [Cold Start] Stock warm-up completed.");
    }
}
