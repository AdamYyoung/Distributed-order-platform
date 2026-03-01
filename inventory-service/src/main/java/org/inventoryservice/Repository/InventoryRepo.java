package org.inventoryservice.Repository;

import org.inventoryservice.DAO.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepo extends JpaRepository<Inventory, Long> {
    @Modifying
    @Query("UPDATE Inventory i SET i.stock = i.stock - :count " +
            "WHERE i.productId = :productId AND i.stock >= :count")
    int atomicDeduct(@Param("productId") Long productId, @Param("count") int count);

    Optional<Inventory> findByProductId(Long productId);

    @Modifying
    @Query("UPDATE Inventory i SET i.stock = i.stock + ?2 WHERE i.productId = ?1")
    int increaseStock(Long productId, int count);

}
