package com.deliveryflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

  public static final String ORDERS_CREATED = "orders.created";
  public static final String ORDERS_STATUS_CHANGED = "orders.status-changed";
  public static final String COURIER_LOCATION_UPDATED = "courier.location-updated";
  public static final String NOTIFICATIONS_SEND = "notifications.send";

  @Bean
  public NewTopic ordersCreatedTopic() {
    return TopicBuilder.name(ORDERS_CREATED).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic ordersStatusChangedTopic() {
    return TopicBuilder.name(ORDERS_STATUS_CHANGED).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic courierLocationUpdatedTopic() {
    return TopicBuilder.name(COURIER_LOCATION_UPDATED).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic notificationsSendTopic() {
    return TopicBuilder.name(NOTIFICATIONS_SEND).partitions(3).replicas(1).build();
  }
}
