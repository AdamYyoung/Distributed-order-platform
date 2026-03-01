package org.orderservice.Service;

import org.orderservice.DTO.CreateOrderResponse;
import org.orderservice.DTO.SeckillResultDTO;

public interface OrderService {
    public SeckillResultDTO getSecKillResult(String orderToken);
    public CreateOrderResponse createOrder(Long productId, int count);
    public CreateOrderResponse createSecKillOrder(Long productId, int count);
    public void handleSagaCompensation(Long orderId, Long userId, Long productId, int count, String resultKey, String orderToken);
}
