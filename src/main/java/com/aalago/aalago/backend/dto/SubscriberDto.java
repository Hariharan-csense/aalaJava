package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record SubscriberDto(
    Object id,
    String email,
    String source,
    Object createdAt) {
  public static SubscriberDto from(Map<String, Object> row) {
    return new SubscriberDto(
        row.get("id"),
        optional(row.get("email"), ""),
        optional(row.get("source"), ""),
        row.get("createdAt"));
  }
}
