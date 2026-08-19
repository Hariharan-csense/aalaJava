package com.aalago.aalago.backend.util;

import com.aalago.aalago.backend.exception.ApiException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

public final class ApiUtil {
  private ApiUtil() {}

  public static Map<String, Object> mapOf(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(String.valueOf(values[i]), values[i + 1]);
    }
    return map;
  }

  public static String required(Object value, String name) {
    String text = optional(value, null);
    if (!StringUtils.hasText(text)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, name + " is required");
    }
    return text.trim();
  }

  public static String optional(Object value, Object fallback) {
    if (value instanceof String s) return s.trim();
    if (value == null) return fallback == null ? "" : String.valueOf(fallback);
    return String.valueOf(value).trim();
  }

  public static BigDecimal number(Object value, Object fallback) {
    Object actual = value == null ? fallback : value;
    try {
      return new BigDecimal(String.valueOf(actual));
    } catch (Exception ex) {
      return BigDecimal.ZERO;
    }
  }

  public static boolean bool(Object value) {
    if (value instanceof Boolean b) return b;
    if (value instanceof Number n) return n.intValue() != 0;
    return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
  }

  public static String email(Object value) {
    String email = required(value, "Email").toLowerCase();
    if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Email must be valid");
    }
    return email;
  }

  public static List<String> stringList(Object value) {
    if (value instanceof List<?> list) {
      return list.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .map(String::trim)
          .filter(StringUtils::hasText)
          .toList();
    }
    if (value instanceof String s) {
      return List.of(s.split(",")).stream().map(String::trim).filter(StringUtils::hasText).toList();
    }
    return List.of();
  }

  public static List<String> strings(Object value) {
    return stringList(value);
  }

  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> list(Object value) {
    if (value instanceof List<?> rows) {
      return rows.stream()
          .filter(Map.class::isInstance)
          .map(row -> (Map<String, Object>) new LinkedHashMap<>((Map<String, Object>) row))
          .toList();
    }
    return List.of();
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> map(Object value) {
    if (value instanceof Map<?, ?> source) return new LinkedHashMap<>((Map<String, Object>) source);
    return new LinkedHashMap<>();
  }

  public static Object value(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? "" : value;
  }

  public static String normalizeBookingUrl(String raw) {
    if (!StringUtils.hasText(raw)) return "";
    try {
      URI uri = URI.create(raw);
      String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase();
      if (!List.of("aalastays.com", "www.aalastays.com", "book.aalabnb.com").contains(host)) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "Booking link must be an AalaStays booking URL");
      }
      return uri.toString();
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Booking link must be a valid URL");
    }
  }

  public static String safeBookingUrl(String raw) {
    try {
      return normalizeBookingUrl(raw);
    } catch (Exception ex) {
      return "";
    }
  }
}
