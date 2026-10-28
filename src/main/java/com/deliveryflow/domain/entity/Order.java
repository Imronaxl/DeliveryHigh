package com.deliveryflow.domain.entity;

import com.deliveryflow.domain.enums.OrderStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Column(name = "courier_id")
  private UUID courierId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private OrderStatus status;

  @Column(name = "pickup_address", nullable = false, length = 500)
  private String pickupAddress;

  @Column(name = "delivery_address", nullable = false, length = 500)
  private String deliveryAddress;

  @Column(name = "pickup_lat", nullable = false)
  private Double pickupLat;

  @Column(name = "pickup_lng", nullable = false)
  private Double pickupLng;

  @Column(name = "delivery_lat", nullable = false)
  private Double deliveryLat;

  @Column(name = "delivery_lng", nullable = false)
  private Double deliveryLng;

  @Column(columnDefinition = "TEXT")
  private String description;

  @CreationTimestamp
  @Column(nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false, columnDefinition = "timestamp with time zone")
  private Instant updatedAt;
}
