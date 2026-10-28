package com.deliveryflow.domain.repository;

import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.domain.enums.OrderStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select o from Order o where o.id = :id")
  java.util.Optional<Order> findLockedById(
      @org.springframework.data.repository.query.Param("id") UUID id);

  List<Order> findByClientId(UUID clientId);

  List<Order> findByCourierId(UUID courierId);

  List<Order> findByStatus(OrderStatus status);

  List<Order> findByCourierIdIsNull();
}
