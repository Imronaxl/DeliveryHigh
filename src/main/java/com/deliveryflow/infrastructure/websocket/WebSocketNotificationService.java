package com.deliveryflow.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.deliveryflow.messaging.event.OrderCreatedEvent;

import java.util.UUID;

@Service
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendOrderStatusUpdate(UUID orderId, Object statusDto) {
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/status", statusDto);
    }

    public void sendCourierLocationUpdate(UUID courierId, Object locationDto) {
        messagingTemplate.convertAndSend("/topic/courier/" + courierId + "/location", locationDto);
    }

    public void broadcastNewOrder(Object orderDto) {
        messagingTemplate.convertAndSend("/topic/orders/available", orderDto);
    }
}
