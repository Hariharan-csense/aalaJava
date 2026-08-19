package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record UploadDto(String filename, String url) {
  public static UploadDto from(Map<String, Object> row) {
    return new UploadDto(optional(row.get("filename"), ""), optional(row.get("url"), ""));
  }
}
