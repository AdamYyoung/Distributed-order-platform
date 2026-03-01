package org.inventoryservice.Service;

import org.inventoryservice.DAO.Inventory;

public interface InventoryService {
    public void warmUp(Long productId);
    public void cacheDeduct(Long productId, int count);
    public void dbDeduct(Long productId, int count, String token);
    public Inventory getByProductId(Long productId);
    public void cacheCompensate(Long productId, int count, String token);
    public void dbCompensate(Long productId, int count, String token);
}
