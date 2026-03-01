package org.inventoryservice.Repository;

import org.inventoryservice.DAO.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryLogRepo extends JpaRepository<InventoryLog, String> {
    @Modifying
    @Query("UPDATE InventoryLog l SET l.status = 2 WHERE l.orderToken = ?1 AND l.status = 1")
    int updateStatusToCompensate(String orderToken);

    @Modifying
    @Query(value = "INSERT INTO inventory_log (order_token, product_id, count, status) VALUES (?1, ?2, ?3, ?4)", nativeQuery = true)
    void insertLogRaw(String token, Long pid, int count, int status);
}
