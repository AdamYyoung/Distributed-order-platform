package org.orderservice.Factory;

import org.orderservice.DAO.Order;

import java.math.BigDecimal;

public interface OrderFactory {
    Order createOrder(Long productId, Integer count, BigDecimal price, Long userId);
}
