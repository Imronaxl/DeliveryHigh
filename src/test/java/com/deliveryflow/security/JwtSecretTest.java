package com.deliveryflow.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtSecretTest {
  @Test
  void rejectsShortSecret() {
    assertThatThrownBy(() -> new JwtTokenProvider("too-short", 60000, 120000))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("32 bytes");
  }
}
