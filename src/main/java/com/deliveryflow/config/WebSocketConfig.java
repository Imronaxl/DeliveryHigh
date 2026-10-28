package com.deliveryflow.config;

import com.deliveryflow.domain.enums.UserRole;
import com.deliveryflow.domain.repository.OrderRepository;
import com.deliveryflow.domain.repository.UserRepository;
import com.deliveryflow.security.JwtTokenProvider;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final JwtTokenProvider tokens;
  private final UserDetailsService details;
  private final UserRepository users;
  private final OrderRepository orders;
  private final Map<String, Principal> sessions = new ConcurrentHashMap<>();

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        new ChannelInterceptor() {
          @Override
          public Message<?> preSend(Message<?> message, MessageChannel channel) {
            var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null) return message;
            if (accessor.getCommand() == StompCommand.CONNECT) {
              String header = accessor.getFirstNativeHeader("Authorization");
              if (header == null || !header.startsWith("Bearer "))
                throw new AccessDeniedException("Token required");
              String token = header.substring(7);
              var user = details.loadUserByUsername(tokens.extractUsername(token));
              if (!tokens.isTokenValid(token, user))
                throw new AccessDeniedException("Invalid token");
              accessor.setUser(
                  new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
              if (accessor.getSessionId() != null)
                sessions.put(accessor.getSessionId(), accessor.getUser());
            }
            if (accessor.getCommand() == StompCommand.SEND)
              throw new AccessDeniedException("Client messages are disabled");
            if (accessor.getCommand() == StompCommand.SUBSCRIBE)
              authorize(accessor.getDestination(), accessor.getUser());
            return message;
          }
        });
  }

  @EventListener
  public void disconnected(SessionDisconnectEvent event) {
    sessions.remove(event.getSessionId());
  }

  @Override
  public void configureClientOutboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        new ChannelInterceptor() {
          @Override
          public Message<?> preSend(Message<?> message, MessageChannel channel) {
            var headers = message.getHeaders();
            String destination = SimpMessageHeaderAccessor.getDestination(headers);
            if (destination == null || !destination.startsWith("/topic/")) return message;
            String session = SimpMessageHeaderAccessor.getSessionId(headers);
            try {
              authorize(destination, session == null ? null : sessions.get(session));
            } catch (AccessDeniedException exception) {
              return null;
            }
            return message;
          }
        });
  }

  private void authorize(String destination, Principal principal) {
    if (principal == null) throw new AccessDeniedException("Authentication required");
    var viewer =
        users
            .findByEmail(principal.getName())
            .orElseThrow(() -> new AccessDeniedException("Unknown user"));
    if ("/topic/admin/orders".equals(destination) && viewer.getRole() == UserRole.ADMIN) return;
    if ("/topic/orders/available".equals(destination)
        && (viewer.getRole() == UserRole.COURIER || viewer.getRole() == UserRole.ADMIN)) return;
    if (destination != null && destination.matches("/topic/orders/[0-9a-fA-F-]{36}/status")) {
      UUID id = UUID.fromString(destination.split("/")[3]);
      var order = orders.findById(id).orElseThrow(() -> new AccessDeniedException("Unknown order"));
      if (viewer.getRole() == UserRole.ADMIN
          || viewer.getId().equals(order.getClientId())
          || viewer.getId().equals(order.getCourierId())) return;
    }
    if (destination != null && destination.matches("/topic/courier/[0-9a-fA-F-]{36}/location")) {
      UUID id = UUID.fromString(destination.split("/")[3]);
      if (viewer.getRole() == UserRole.ADMIN
          || viewer.getId().equals(id)
          || orders.findByCourierId(id).stream()
              .anyMatch(
                  order ->
                      viewer.getId().equals(order.getClientId())
                          && order.getStatus()
                              != com.deliveryflow.domain.enums.OrderStatus.DELIVERED
                          && order.getStatus()
                              != com.deliveryflow.domain.enums.OrderStatus.CANCELLED)) return;
    }
    throw new AccessDeniedException("Subscription denied");
  }
}
