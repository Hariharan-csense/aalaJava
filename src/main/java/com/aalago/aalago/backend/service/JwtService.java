package com.aalago.aalago.backend.service;

import com.aalago.aalago.backend.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final ObjectMapper mapper;
  private final String secret;

  public JwtService(ObjectMapper mapper, @Value("${aalago.jwt.secret}") String secret) {
    this.mapper = mapper;
    this.secret = secret;
  }

  public String sign(String email) {
    try {
      String header = b64(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
      String payload = b64(mapper.writeValueAsBytes(Map.of("email", email, "exp", Instant.now().plusSeconds(7 * 24 * 3600).getEpochSecond())));
      return header + "." + payload + "." + hmac(header + "." + payload);
    } catch (Exception ex) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
  }

  public Optional<String> email(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3 || !MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), hmac(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8))) {
        return Optional.empty();
      }
      JsonNode payload = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
      if (payload.path("exp").asLong(0) < Instant.now().getEpochSecond()) return Optional.empty();
      return Optional.ofNullable(payload.path("email").asText(null));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  private String hmac(String input) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return b64(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
  }

  private static String b64(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
