package org.orderservice.Proxy;

import lombok.RequiredArgsConstructor;
import org.orderservice.DAO.Order;
import org.orderservice.Factory.OrderFactory;
import org.orderservice.Repository.OrderRepo;
import org.orderservice.Service.OrderService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderServiceProxy {
    private final OrderRepo orderRepo;
    private final OrderFactory orderFactory;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order savePendingOrder(Long userId, Long productId, int count, BigDecimal price) {
        Order order = orderFactory.createOrder(productId, count, price, userId);
        order.setStatus("PENDING");
        return orderRepo.saveAndFlush(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOrderStatus(Long orderId, String status) {
        orderRepo.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            orderRepo.saveAndFlush(order);
        });
    }
}
