package com.deliveryflow.api;

import static org.assertj.core.api.Assertions.*;

import com.deliveryflow.infrastructure.websocket.WebSocketNotificationService;
import java.lang.reflect.Type;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:websocket;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.kafka.listener.auto-startup=false",
      "spring.kafka.admin.auto-create=false"
    })
class WebSocketDeliveryFlowTest extends DeliveryFlowTest {
  @LocalServerPort int port;
  @Autowired WebSocketNotificationService notifications;
  @Autowired org.springframework.messaging.simp.user.SimpUserRegistry registry;

  @Test
  void authenticatedClientReceivesOwnOrderUpdate() throws Exception {
    var ids = createSocketOrder();
    String token =
        json.readTree(
                mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/v1/auth/login")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(
                                "{\"email\":\"socket@test.com\",\"password\":\"password123\"}"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("accessToken")
            .asText();
    var client = new WebSocketStompClient(new StandardWebSocketClient());
    var headers = new StompHeaders();
    headers.add("Authorization", "Bearer " + token);
    StompSession session = null;
    try {
      session =
          client
              .connectAsync(
                  "ws://127.0.0.1:" + port + "/ws",
                  new org.springframework.web.socket.WebSocketHttpHeaders(),
                  headers,
                  new StompSessionHandlerAdapter() {})
              .get(10, TimeUnit.SECONDS);
      var received = new CompletableFuture<byte[]>();
      var subscription =
          session.subscribe(
              "/topic/orders/" + ids + "/status",
              new StompFrameHandler() {
                public Type getPayloadType(StompHeaders headers) {
                  return byte[].class;
                }

                public void handleFrame(StompHeaders headers, Object payload) {
                  received.complete((byte[]) payload);
                }
              });
      org.awaitility.Awaitility.await()
          .atMost(10, TimeUnit.SECONDS)
          .until(
              () ->
                  registry
                          .findSubscriptions(
                              sub ->
                                  sub.getDestination().equals("/topic/orders/" + ids + "/status"))
                          .size()
                      == 1);
      notifications.sendOrderStatusUpdate(ids, java.util.Map.of("newStatus", "ASSIGNED"));
      assertThat(
              new String(
                  received.get(10, TimeUnit.SECONDS), java.nio.charset.StandardCharsets.UTF_8))
          .contains("ASSIGNED");
    } finally {
      if (session != null) session.disconnect();
      client.stop();
    }
  }

  java.util.UUID createSocketOrder() throws Exception {
    register("socket@test.com", "CLIENT");
    var client = users.findByEmail("socket@test.com").orElseThrow();
    return orderService.createOrder(client.getId(), "A", "B", 55, 37, 56, 38, null).getId();
  }
}
