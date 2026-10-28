package com.deliveryflow.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.deliveryflow.domain.entity.User;
import com.deliveryflow.domain.enums.UserRole;
import com.deliveryflow.domain.repository.UserRepository;
import com.deliveryflow.messaging.producer.EventOutbox;
import com.fasterxml.jackson.databind.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:delivery;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.kafka.listener.auto-startup=false",
      "spring.kafka.admin.auto-create=false"
    })
@AutoConfigureMockMvc
class DeliveryFlowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @MockBean EventOutbox outbox;

  String register(String email, String role) throws Exception {
    String body =
        json.writeValueAsString(
            Map.of("email", email, "password", "password123", "name", "User", "role", role));
    return json.readTree(
            mvc.perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .get("accessToken")
        .asText();
  }

  @Test
  void deliveryLifecycleAndAccessRules() throws Exception {
    String client = register("client@test.com", "CLIENT"),
        courier = register("courier@test.com", "COURIER"),
        stranger = register("stranger@test.com", "CLIENT"),
        otherCourier = register("other@test.com", "COURIER");
    String body =
        "{\"pickupAddress\":\"A\",\"deliveryAddress\":\"B\",\"pickupLat\":55.75,\"pickupLng\":37.61,\"deliveryLat\":55.76,\"deliveryLng\":37.62}";
    String id =
        json.readTree(
                mvc.perform(
                        post("/api/v1/orders")
                            .header("Authorization", "Bearer " + client)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("id")
            .asText();
    mvc.perform(get("/api/v1/orders/" + id).header("Authorization", "Bearer " + stranger))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/v1/courier/orders/" + id + "/accept")
                .header("Authorization", "Bearer " + courier))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ASSIGNED"));
    mvc.perform(
            patch("/api/v1/courier/orders/" + id + "/accept")
                .header("Authorization", "Bearer " + otherCourier))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/v1/courier/orders/" + id + "/status")
                .header("Authorization", "Bearer " + otherCourier)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PICKED_UP\"}"))
        .andExpect(status().isForbidden());
    for (String state : List.of("PICKED_UP", "IN_TRANSIT", "DELIVERED")) {
      mvc.perform(
              patch("/api/v1/courier/orders/" + id + "/status")
                  .header("Authorization", "Bearer " + courier)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"status\":\"" + state + "\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value(state));
    }
    mvc.perform(delete("/api/v1/orders/" + id).header("Authorization", "Bearer " + client))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/courier/orders/active").header("Authorization", "Bearer " + courier))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void publicCannotRegisterAdmin() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"admin@test.com\",\"password\":\"password123\",\"name\":\"Admin\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void anonymousGetsUnauthorized() throws Exception {
    mvc.perform(get("/api/v1/orders/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsCoordinatesOutsideRange() throws Exception {
    String client = register("coordinates@test.com", "CLIENT");
    mvc.perform(
            post("/api/v1/orders")
                .header("Authorization", "Bearer " + client)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"pickupAddress\":\"A\",\"deliveryAddress\":\"B\",\"pickupLat\":100,\"pickupLng\":0,\"deliveryLat\":0,\"deliveryLng\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Autowired com.deliveryflow.service.OrderService orderService;
  @Autowired com.deliveryflow.domain.repository.OrderRepository orders;

  @Test
  void onlyOneCourierWinsConcurrentAssignment() throws Exception {
    UUIDHolder ids = createConcurrentOrder();
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var gate = new java.util.concurrent.CountDownLatch(1);
      var tasks =
          List.of(ids.first(), ids.second()).stream()
              .map(
                  courier ->
                      executor.submit(
                          () -> {
                            gate.await();
                            try {
                              orderService.assignCourier(ids.order(), courier);
                              return true;
                            } catch (
                                com.deliveryflow.api.exception.InvalidStatusTransitionException
                                    ex) {
                              return false;
                            }
                          }))
              .toList();
      gate.countDown();
      int winners = 0;
      for (var task : tasks) if (task.get(10, TimeUnit.SECONDS)) winners++;
      org.assertj.core.api.Assertions.assertThat(winners).isEqualTo(1);
      org.assertj.core.api.Assertions.assertThat(
              orders.findById(ids.order()).orElseThrow().getCourierId())
          .isIn(ids.first(), ids.second());
    }
  }

  record UUIDHolder(UUID order, UUID first, UUID second) {}

  UUIDHolder createConcurrentOrder() {
    var client =
        users.save(
            User.builder()
                .email("race-client@test.com")
                .password("hash")
                .name("Client")
                .role(UserRole.CLIENT)
                .build());
    var first =
        users.save(
            User.builder()
                .email("race-first@test.com")
                .password("hash")
                .name("First")
                .role(UserRole.COURIER)
                .build());
    var second =
        users.save(
            User.builder()
                .email("race-second@test.com")
                .password("hash")
                .name("Second")
                .role(UserRole.COURIER)
                .build());
    var order = orderService.createOrder(client.getId(), "A", "B", 55, 37, 56, 38, null);
    return new UUIDHolder(order.getId(), first.getId(), second.getId());
  }
}
