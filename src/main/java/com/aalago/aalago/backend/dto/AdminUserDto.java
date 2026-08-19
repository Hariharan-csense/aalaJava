package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record AdminUserDto(String email) {
  public static AdminUserDto from(Map<String, Object> row) {
    return new AdminUserDto(optional(row.get("email"), ""));
  }
}
