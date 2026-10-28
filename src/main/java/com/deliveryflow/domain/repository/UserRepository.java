package com.deliveryflow.domain.repository;

import com.deliveryflow.domain.entity.User;
import com.deliveryflow.domain.enums.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  List<User> findByRole(UserRole role);
}
