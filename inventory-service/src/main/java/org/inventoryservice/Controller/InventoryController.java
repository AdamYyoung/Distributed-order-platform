package org.inventoryservice.Controller;

import lombok.RequiredArgsConstructor;
import org.commonlib.Response.ApiResponse;
import org.inventoryservice.DAO.Inventory;
import org.inventoryservice.DTO.OrderRequest;
import org.inventoryservice.Service.InventoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;
    @GetMapping("/{productId}")
    public ApiResponse<Inventory> getInventory(@PathVariable Long productId) {
        return ApiResponse.success(inventoryService.getByProductId(productId));
    }

    @PostMapping("/deduct")
    public ApiResponse<Void> deduct(@RequestBody OrderRequest req) {
        inventoryService.cacheDeduct(req.getProductId(), req.getCount());
        inventoryService.dbDeduct(req.getProductId(), req.getCount(), req.getOrderToken());
        return ApiResponse.success(null);
    }

    @PostMapping("/cacheCompensate")
    public ApiResponse<Void> cacheCompensate(@RequestBody OrderRequest req) {
        inventoryService.cacheCompensate(req.getProductId(), req.getCount(), req.getOrderToken());
        return ApiResponse.success(null);
    }
    @PostMapping("/dbCompensate")
    public ApiResponse<Void> dbCompensate(@RequestBody OrderRequest req) {
        inventoryService.dbCompensate(req.getProductId(), req.getCount(), req.getOrderToken());
        return ApiResponse.success(null);
    }

    @PostMapping("/cacheDeduct")
    public ApiResponse<Void> cacheDeduct(@RequestBody OrderRequest req) {
        inventoryService.cacheDeduct(req.getProductId(), req.getCount());
        return ApiResponse.success(null);
    }

    @PostMapping("/dbDeduct")
    public ApiResponse<Void> dbDeduct(@RequestBody OrderRequest req) {
        inventoryService.dbDeduct(req.getProductId(), req.getCount(), req.getOrderToken());
        return ApiResponse.success(null);
    }
}
