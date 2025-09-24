package com.location.server.api;

import com.location.server.security.JwtService;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

  record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  record LoginResponse(String token, long expiresAt) {}

  private final JwtService jwt;

  public AuthController(JwtService jwt) {
    this.jwt = jwt;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
    // Diff 1: accepte identifiants non vides, pas de DB.
    var exp = Instant.now().plusSeconds(3600).getEpochSecond();
    var token = jwt.generateToken(Map.of("sub", req.username()), exp);
    return ResponseEntity.ok(new LoginResponse(token, exp));
  }
}
