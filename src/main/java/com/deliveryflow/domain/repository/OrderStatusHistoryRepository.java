package com.deliveryflow.domain.repository;

import com.deliveryflow.domain.entity.OrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

  List<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(UUID orderId);
}
