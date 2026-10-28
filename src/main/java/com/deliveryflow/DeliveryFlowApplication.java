package com.deliveryflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeliveryFlowApplication {
  public static void main(String[] args) {
    SpringApplication.run(DeliveryFlowApplication.class, args);
  }
}
