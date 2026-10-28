package com.deliveryflow.messaging.event;

import java.util.UUID;

public record NotificationSendEvent(UUID userId, String message, NotificationType type) {}
