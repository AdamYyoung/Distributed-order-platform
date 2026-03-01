package org.orderservice.Factory;

import jdk.jfr.Category;
import org.orderservice.DAO.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NormalOrderFactory implements OrderFactory {
    @Override
    public Order createOrder(Long productId, Integer count, BigDecimal price, Long userId) {
        Order order = new Order();
        order.setProductId(productId);
        order.setCount(count);
        order.setAmount(price.multiply(BigDecimal.valueOf(count)));
        order.setUserId(userId);
        order.setStatus("PROCESSING");
        return order;
    }
}
