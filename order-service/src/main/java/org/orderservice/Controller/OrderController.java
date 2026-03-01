package org.orderservice.Controller;

import lombok.RequiredArgsConstructor;
import org.commonlib.Config.EnableCommonConfig;
import org.commonlib.Response.ApiResponse;
import org.orderservice.DTO.CreateOrderRequest;
import org.orderservice.DTO.CreateOrderResponse;
import org.orderservice.DTO.SeckillResultDTO;
import org.orderservice.Service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@EnableCommonConfig
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ApiResponse<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest req) {
        return ApiResponse.success(
                orderService.createOrder(
                        req.getProductId(),
                        req.getCount()
                )
        );
    }

    @PostMapping("/seckill")
    public ApiResponse<CreateOrderResponse> createSeckillOrder(@RequestBody CreateOrderRequest req) {
        return ApiResponse.success(orderService.createSecKillOrder(req.getProductId(), req.getCount()));
    }

    @GetMapping("/seckill/result/{orderToken}")
    public ApiResponse<SeckillResultDTO> getSeckillResult(@PathVariable String orderToken) {
        return ApiResponse.success(orderService.getSecKillResult(orderToken));
    }
}
