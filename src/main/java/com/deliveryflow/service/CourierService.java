package com.deliveryflow.service;

import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.domain.entity.User;
import com.deliveryflow.domain.enums.OrderStatus;
import com.deliveryflow.domain.enums.UserRole;
import com.deliveryflow.domain.repository.OrderRepository;
import com.deliveryflow.domain.repository.UserRepository;
import com.deliveryflow.infrastructure.redis.CourierGeoRepository;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CourierService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CourierGeoRepository courierGeoRepository;

    public CourierService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            CourierGeoRepository courierGeoRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.courierGeoRepository = courierGeoRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> findAvailableOrders() {
        return orderRepository.findByStatus(OrderStatus.CREATED);
    }

    @Transactional(readOnly = true)
    public List<Order> findActiveOrders(UUID courierId) {
        return orderRepository.findByCourierId(courierId).stream()
                .filter(order -> order.getStatus() != OrderStatus.DELIVERED
                        && order.getStatus() != OrderStatus.CANCELLED)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<User> findAllCouriers() {
        return userRepository.findByRole(UserRole.COURIER);
    }

    @Transactional(readOnly = true)
    public List<GeoResult<String>> findNearbyCouriers(double longitude, double latitude, double radiusKm) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = courierGeoRepository.findNearbyCouriers(
                longitude,
                latitude,
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        if (results == null) {
            return List.of();
        }

        return results.getContent();
    }
}
