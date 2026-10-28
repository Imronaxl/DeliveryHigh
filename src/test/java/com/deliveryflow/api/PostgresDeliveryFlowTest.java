package com.deliveryflow.api;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers(disabledWithoutDocker = true)
class PostgresDeliveryFlowTest extends DeliveryFlowTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
    properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    properties.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    properties.add(
        "spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    properties.add("spring.flyway.enabled", () -> "true");
  }
}
