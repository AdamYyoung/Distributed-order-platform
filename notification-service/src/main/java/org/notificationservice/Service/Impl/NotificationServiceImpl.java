package org.notificationservice.Service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonlib.DTO.OrderEvent;
import org.commonlib.Threadlocal.TraceIdHolder;
import org.notificationservice.Service.NotificationService;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    public void sendNotification(OrderEvent orderEvent) {
        log.info("3. send notification for " + orderEvent.getOrderId());
        try{
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("4. send notification succeed!", orderEvent.getOrderId());
        log.info("Current Thread: {}, TraceId in MDC: {}",
                Thread.currentThread().getName(),
                MDC.get(TraceIdHolder.MDC_KEY));
    }
}
