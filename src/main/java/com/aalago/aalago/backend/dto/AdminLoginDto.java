package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record AdminLoginDto(String token, AdminUserDto user) {
  public static AdminLoginDto from(Map<String, Object> row) {
    return new AdminLoginDto(optional(row.get("token"), ""), AdminUserDto.from(map(row.get("user"))));
  }
}
