package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record RazorpayCheckoutDto(
    String keyId,
    String orderId,
    Number amount,
    String currency,
    String name,
    String description) {
  public static RazorpayCheckoutDto from(Map<String, Object> row) {
    return new RazorpayCheckoutDto(
        optional(row.get("keyId"), ""),
        optional(row.get("orderId"), ""),
        number(row.get("amount"), 0),
        optional(row.get("currency"), ""),
        optional(row.get("name"), ""),
        optional(row.get("description"), ""));
  }
}
