package com.location.server.config;

import com.location.server.repo.AgencyRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AgencyRepository agencyRepository;

  public WebConfig(AgencyRepository agencyRepository) {
    this.agencyRepository = agencyRepository;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new AgencyHeaderInterceptor(agencyRepository));
  }
}
