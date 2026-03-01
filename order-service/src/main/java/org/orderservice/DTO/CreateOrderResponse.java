package org.orderservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CreateOrderResponse {
    private Long orderId;
    private BigDecimal amount;
    private String status;
    private String token;
}
