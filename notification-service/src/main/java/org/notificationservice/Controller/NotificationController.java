package org.notificationservice.Controller;

import lombok.RequiredArgsConstructor;
import org.commonlib.Response.ApiResponse;
import org.commonlib.DTO.OrderEvent;
import org.notificationservice.Service.NotificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping("/order-created")
    public ApiResponse<Void> handleOrderCreated(@RequestBody OrderEvent orderEvent) {
        notificationService.sendNotification(orderEvent);
        return ApiResponse.success(null);
    }
}
