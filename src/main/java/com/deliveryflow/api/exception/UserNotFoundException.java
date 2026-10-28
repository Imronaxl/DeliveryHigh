package com.deliveryflow.api.exception;

import java.util.UUID;

public class UserNotFoundException extends AppException {

  public UserNotFoundException(String message) {
    super(message);
  }

  public static UserNotFoundException byEmail(String email) {
    return new UserNotFoundException("User not found with email: " + email);
  }

  public static UserNotFoundException byId(UUID id) {
    return new UserNotFoundException("User not found with id: " + id);
  }
}
