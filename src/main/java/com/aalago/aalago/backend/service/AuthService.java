package com.aalago.aalago.backend.service;

import static com.aalago.aalago.backend.util.ApiUtil.mapOf;

import com.aalago.aalago.backend.exception.ApiException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
  private final StoreService store;
  private final JwtService jwt;
  private final String adminEmail;
  private final String adminPassword;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthService(
      StoreService store,
      JwtService jwt,
      @Value("${ADMIN_EMAIL:}") String adminEmail,
      @Value("${ADMIN_PASSWORD:}") String adminPassword) {
    this.store = store;
    this.jwt = jwt;
    this.adminEmail = adminEmail;
    this.adminPassword = adminPassword;
  }

  public Map<String, Object> login(String email, String password) {
    if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Email and password required");
    }
    Map<String, Object> admin = store.admin();
    String effectiveEmail = effectiveEmail(admin);
    boolean passwordOk = StringUtils.hasText(adminPassword)
        ? password.equals(adminPassword)
        : encoder.matches(password, String.valueOf(admin.get("passwordHash")));
    if (!email.equals(effectiveEmail) || !passwordOk) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    return mapOf("token", jwt.sign(email), "user", Map.of("email", email));
  }

  public boolean valid(String token) {
    Map<String, Object> admin = store.admin();
    return jwt.email(token).map(email -> email.equals(effectiveEmail(admin))).orElse(false);
  }

  private String effectiveEmail(Map<String, Object> admin) {
    return StringUtils.hasText(adminEmail) ? adminEmail : String.valueOf(admin.get("email"));
  }
}
