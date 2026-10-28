package com.deliveryflow.security;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

class JwtTokenProviderTest {
  JwtTokenProvider tokens =
      new JwtTokenProvider("test-secret-with-at-least-thirty-two-bytes", 60000, 120000);
  org.springframework.security.core.userdetails.UserDetails user =
      User.withUsername("client@example.com").password("hash").roles("CLIENT").build();

  @Test
  void acceptsAccessToken() {
    assertThat(tokens.isTokenValid(tokens.generateToken(user), user)).isTrue();
  }

  @Test
  void refreshCannotBeUsedAsAccess() {
    String token = tokens.generateRefreshToken(user);
    assertThat(tokens.isRefreshToken(token)).isTrue();
    assertThat(tokens.isTokenValid(token, user)).isFalse();
  }

  @Test
  void rejectsTampering() {
    String token = tokens.generateToken(user);
    assertThatThrownBy(
            () ->
                tokens.extractUsername(token.substring(0, token.lastIndexOf('.') + 1) + "invalid"))
        .isInstanceOf(io.jsonwebtoken.JwtException.class);
  }
}
