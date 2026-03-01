package org.orderservice.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    // private String description;
    private Integer status;
    private LocalDateTime created_time;
}
