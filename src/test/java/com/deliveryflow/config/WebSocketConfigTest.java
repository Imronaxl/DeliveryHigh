package com.deliveryflow.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.deliveryflow.domain.entity.*;
import com.deliveryflow.domain.enums.*;
import com.deliveryflow.domain.repository.*;
import com.deliveryflow.security.JwtTokenProvider;
import java.util.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;

class WebSocketConfigTest {
  UserRepository users = mock(UserRepository.class);
  OrderRepository orders = mock(OrderRepository.class);
  UUID viewer = UUID.randomUUID(), courier = UUID.randomUUID(), id = UUID.randomUUID();
  WebSocketConfig config =
      new WebSocketConfig(
          new JwtTokenProvider("secret-at-least-thirty-two-bytes-long", 60000, 120000),
          mock(UserDetailsService.class),
          users,
          orders);

  static class Registry extends ChannelRegistration {
    List<ChannelInterceptor> filters() {
      return getInterceptors();
    }
  }

  Message<?> message(StompCommand command, String destination) {
    var accessor = StompHeaderAccessor.create(command);
    accessor.setUser(
        new UsernamePasswordAuthenticationToken("client@example.com", null, List.of()));
    if (destination != null) accessor.setDestination(destination);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  ChannelInterceptor guard() {
    var registration = new Registry();
    config.configureClientInboundChannel(registration);
    return registration.filters().getFirst();
  }

  @Test
  void deniesUnauthenticatedConnect() {
    assertThatThrownBy(
            () -> guard().preSend(message(StompCommand.CONNECT, null), mock(MessageChannel.class)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void allowsOwnOrderSubscription() {
    when(users.findByEmail("client@example.com"))
        .thenReturn(Optional.of(User.builder().id(viewer).role(UserRole.CLIENT).build()));
    when(orders.findById(id))
        .thenReturn(Optional.of(Order.builder().id(id).clientId(viewer).build()));
    assertThat(
            guard()
                .preSend(
                    message(StompCommand.SUBSCRIBE, "/topic/orders/" + id + "/status"),
                    mock(MessageChannel.class)))
        .isNotNull();
  }

  @Test
  void deniesAnotherClientsOrderSubscription() {
    when(users.findByEmail("client@example.com"))
        .thenReturn(Optional.of(User.builder().id(viewer).role(UserRole.CLIENT).build()));
    when(orders.findById(id))
        .thenReturn(Optional.of(Order.builder().id(id).clientId(UUID.randomUUID()).build()));
    assertThatThrownBy(
            () ->
                guard()
                    .preSend(
                        message(StompCommand.SUBSCRIBE, "/topic/orders/" + id + "/status"),
                        mock(MessageChannel.class)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void deniesClientPublishing() {
    assertThatThrownBy(
            () ->
                guard()
                    .preSend(
                        message(StompCommand.SEND, "/topic/orders/available"),
                        mock(MessageChannel.class)))
        .isInstanceOf(AccessDeniedException.class);
  }
}
