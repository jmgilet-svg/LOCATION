package com.location.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final Key key;

  public JwtService(@Value("${app.jwt.secret:}") String secret) {
    String envSecret =
        System.getenv().getOrDefault("JWT_SECRET", "dev-secret-please-change-dev-secret");
    String effective = (secret != null && !secret.isBlank()) ? secret : envSecret;
    // Ensure length OK for HS256 key
    if (effective.length() < 32) {
      effective = (effective + "................................").substring(0, 32);
    }
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodeBase64(effective)));
  }

  private static String encodeBase64(String s) {
    return java.util.Base64.getEncoder().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  public String generateToken(Map<String, Object> claims, long expiresAtEpochSeconds) {
    return Jwts.builder()
        .setClaims(claims)
        .setExpiration(new Date(expiresAtEpochSeconds * 1000))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseToken(String token) {
    try {
      return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    } catch (Exception e) {
      return null;
    }
  }
}
