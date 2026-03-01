package org.orderservice.DTO;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long productId;
    private Integer count;
}
