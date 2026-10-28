package com.deliveryflow.messaging.producer;

import com.deliveryflow.messaging.event.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EventOutbox {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final KafkaTemplate<String, Object> kafka;
  private static final Map<String, Class<?>> TYPES =
      Map.of(
          OrderCreatedEvent.class.getName(), OrderCreatedEvent.class,
          OrderStatusChangedEvent.class.getName(), OrderStatusChangedEvent.class,
          CourierLocationUpdatedEvent.class.getName(), CourierLocationUpdatedEvent.class,
          NotificationSendEvent.class.getName(), NotificationSendEvent.class);

  @Transactional
  public void append(String topic, String key, Object event) {
    try {
      jdbc.update(
          "insert into event_outbox(topic,event_key,event_type,payload) values (?,?,?,?)",
          topic,
          key,
          event.getClass().getName(),
          json.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot serialize event", e);
    }
  }

  @Scheduled(fixedDelayString = "${outbox.interval-ms:500}")
  @Transactional
  public void publish() {
    var rows =
        jdbc.queryForList(
            "select * from event_outbox order by id limit 100 for update skip locked");
    for (var row : rows) {
      try {
        var type = TYPES.get(row.get("event_type").toString());
        if (type == null) throw new IllegalStateException("Unknown event type");
        Object event = json.readValue(row.get("payload").toString(), type);
        kafka
            .send(row.get("topic").toString(), row.get("event_key").toString(), event)
            .get(10, TimeUnit.SECONDS);
        jdbc.update("delete from event_outbox where id = ?", row.get("id"));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Event publishing interrupted", e);
      } catch (Exception e) {
        throw new IllegalStateException("Event publishing failed", e);
      }
    }
  }
}
