package com.deliveryflow.api.exception;

public class InvalidStatusTransitionException extends AppException {

  public InvalidStatusTransitionException(String message) {
    super(message);
  }

  public static InvalidStatusTransitionException fromTo(
      com.deliveryflow.domain.enums.OrderStatus from,
      com.deliveryflow.domain.enums.OrderStatus to) {
    return new InvalidStatusTransitionException(
        "Invalid status transition from " + from + " to " + to);
  }
}
