package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record BlogPostDto(
    String id,
    String title,
    String excerpt,
    String author,
    String readTime,
    String category,
    String date,
    String image) {
  public static BlogPostDto from(Map<String, Object> row) {
    return new BlogPostDto(
        optional(row.get("id"), ""),
        optional(row.get("title"), ""),
        optional(row.get("excerpt"), ""),
        optional(row.get("author"), ""),
        optional(row.get("readTime"), ""),
        optional(row.get("category"), ""),
        optional(row.get("date"), ""),
        optional(row.get("image"), ""));
  }
}
