package com.location.server.config;

import com.location.server.service.MailGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {
  @Bean
  public MailGateway mailGateway() {
    return new MailGateway.DevMailGateway();
  }
}
