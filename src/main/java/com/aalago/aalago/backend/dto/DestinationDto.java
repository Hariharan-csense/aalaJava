package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record DestinationDto(
    String id,
    String name,
    String state,
    String image,
    String description,
    Number properties) {
  public static DestinationDto from(Map<String, Object> row) {
    return new DestinationDto(
        optional(row.get("id"), ""),
        optional(row.get("name"), ""),
        optional(row.get("state"), ""),
        optional(row.get("image"), ""),
        optional(row.get("description"), ""),
        number(row.get("properties"), 0));
  }
}
