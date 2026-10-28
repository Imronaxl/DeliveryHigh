package com.deliveryflow.security;

import com.deliveryflow.api.exception.UserNotFoundException;
import com.deliveryflow.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {
  private final UserRepository users;

  public UUID id(UserDetails principal) {
    return users
        .findByEmail(principal.getUsername())
        .orElseThrow(() -> UserNotFoundException.byEmail(principal.getUsername()))
        .getId();
  }
}
