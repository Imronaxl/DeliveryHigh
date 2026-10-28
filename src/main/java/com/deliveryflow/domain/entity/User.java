package com.deliveryflow.domain.entity;

import com.deliveryflow.domain.enums.UserRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(nullable = false)
  @lombok.ToString.Exclude
  private String password;

  @Column(nullable = false)
  private String name;

  @Column(length = 50)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;

  @CreationTimestamp
  @Column(nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
  private Instant createdAt;
}
