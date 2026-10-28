package com.deliveryflow.config;

import com.deliveryflow.domain.entity.User;
import com.deliveryflow.domain.enums.UserRole;
import com.deliveryflow.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
  @Bean
  ApplicationRunner createAdmin(
      UserRepository users,
      PasswordEncoder encoder,
      @Value("${admin.email:}") String email,
      @Value("${admin.password:}") String password) {
    return args -> {
      if (email.isBlank() && password.isBlank()) return;
      if (email.isBlank() || password.length() < 12 || password.length() > 72) {
        throw new IllegalArgumentException(
            "ADMIN_EMAIL and a 12–72 character ADMIN_PASSWORD are required");
      }
      var existing = users.findByEmail(email);
      if (existing.isPresent()) {
        if (existing.get().getRole() != UserRole.ADMIN)
          throw new IllegalStateException("Admin email belongs to another role");
        return;
      }
      users.save(
          User.builder()
              .email(email)
              .password(encoder.encode(password))
              .name("Administrator")
              .role(UserRole.ADMIN)
              .build());
    };
  }
}
