package com.deliveryflow.api.exception;

import java.util.UUID;

public class OrderNotFoundException extends AppException {

  public OrderNotFoundException(String message) {
    super(message);
  }

  public static OrderNotFoundException byId(UUID id) {
    return new OrderNotFoundException("Order not found with id: " + id);
  }
}
