package com.location.server.config;

import com.location.server.api.AgencyContext;
import com.location.server.repo.AgencyRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

public class AgencyHeaderInterceptor implements HandlerInterceptor {
  private final AgencyRepository agencyRepository;

  public AgencyHeaderInterceptor(AgencyRepository agencyRepository) {
    this.agencyRepository = agencyRepository;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String path = request.getRequestURI();
    if (path == null || !path.startsWith("/api/v1/")) {
      return true;
    }
    String agencyId = request.getHeader("X-Agency-Id");
    if (agencyId == null || agencyId.isBlank()) {
      response.sendError(HttpStatus.BAD_REQUEST.value(), "Header X-Agency-Id is required");
      return false;
    }
    if (!agencyRepository.existsById(agencyId)) {
      response.sendError(HttpStatus.FORBIDDEN.value(), "Unknown agency");
      return false;
    }
    AgencyContext.set(agencyId);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    AgencyContext.clear();
  }
}
