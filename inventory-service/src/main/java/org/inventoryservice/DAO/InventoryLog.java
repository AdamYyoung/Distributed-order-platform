package org.inventoryservice.DAO;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "inventory_log")
@AllArgsConstructor
@NoArgsConstructor
public class InventoryLog {
    @Id
    private String orderToken;
    private Long productId;
    private Integer count;
    private Integer status; // 1-DEDUCTED, 2-COMPENSATED
}
