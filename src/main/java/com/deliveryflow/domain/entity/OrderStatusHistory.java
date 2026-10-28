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

@Entity
@Table(name = "order_status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private OrderStatus status;

  @CreationTimestamp
  @Column(nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
  private Instant changedAt;

  @Column(name = "changed_by_id", nullable = false)
  private UUID changedById;
}
