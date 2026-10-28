package com.deliveryflow.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class VirtualThreadConfig {

  @Bean(name = "virtualTaskExecutor")
  public Executor virtualTaskExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
