package com.location.server.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Tiny per-IP rate limiter for /api/v1/mail/* to avoid abuse. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
  private static final Map<String, AtomicInteger> COUNTS = new ConcurrentHashMap<>();
  private static final Map<String, Long> WINDOWS = new ConcurrentHashMap<>();
  private static final int LIMIT = 20; // 20 req / 60s
  private static final long WINDOW_MS = 60_000L;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path == null || !path.startsWith("/api/v1/mail/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String ip = request.getRemoteAddr();
    long now = System.currentTimeMillis();
    long windowStart = WINDOWS.getOrDefault(ip, 0L);
    AtomicInteger counter = COUNTS.computeIfAbsent(ip, key -> new AtomicInteger());
    if (now - windowStart > WINDOW_MS) {
      WINDOWS.put(ip, now);
      counter.set(0);
      windowStart = now;
    }
    int current = counter.incrementAndGet();
    if (current > LIMIT) {
      long retryInSeconds = Math.max(1L, (WINDOW_MS - (now - windowStart)) / 1000L);
      response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response
          .getWriter()
          .write("{\"error\":\"rate_limited\",\"retry_in\":\"" + retryInSeconds + "s\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
