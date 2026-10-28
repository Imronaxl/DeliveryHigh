package com.deliveryflow.messaging;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.deliveryflow.messaging.event.OrderCreatedEvent;
import com.deliveryflow.messaging.producer.EventOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

class EventOutboxTest {
  JdbcTemplate jdbc = mock(JdbcTemplate.class);

  @SuppressWarnings("unchecked")
  KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);

  ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
  EventOutbox outbox = new EventOutbox(jdbc, json, kafka);

  @Test
  void deletesOnlyAfterBrokerAcknowledges() throws Exception {
    var event =
        new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), "A", "B", Instant.now());
    when(jdbc.queryForList(anyString()))
        .thenReturn(
            List.of(
                Map.of(
                    "id",
                    1L,
                    "topic",
                    "orders.created",
                    "event_key",
                    event.orderId().toString(),
                    "event_type",
                    event.getClass().getName(),
                    "payload",
                    json.writeValueAsString(event))));
    when(kafka.send(anyString(), anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    outbox.publish();
    var order = inOrder(kafka, jdbc);
    order.verify(jdbc).queryForList(anyString());
    order
        .verify(kafka)
        .send(eq("orders.created"), eq(event.orderId().toString()), any(OrderCreatedEvent.class));
    order.verify(jdbc).update("delete from event_outbox where id = ?", 1L);
  }

  @Test
  void failedPublishKeepsPendingRow() throws Exception {
    var event =
        new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), "A", "B", Instant.now());
    when(jdbc.queryForList(anyString()))
        .thenReturn(
            List.of(
                Map.of(
                    "id",
                    1L,
                    "topic",
                    "orders.created",
                    "event_key",
                    "key",
                    "event_type",
                    event.getClass().getName(),
                    "payload",
                    json.writeValueAsString(event))));
    when(kafka.send(anyString(), anyString(), any()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("offline")));
    assertThatThrownBy(outbox::publish).isInstanceOf(IllegalStateException.class);
    verify(jdbc, never()).update(eq("delete from event_outbox where id = ?"), any(Object.class));
  }
}
