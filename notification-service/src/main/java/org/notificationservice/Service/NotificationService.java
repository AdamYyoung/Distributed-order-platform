package org.notificationservice.Service;

import org.commonlib.DTO.OrderEvent;

public interface NotificationService {
    public void sendNotification(OrderEvent orderEvent);
}
