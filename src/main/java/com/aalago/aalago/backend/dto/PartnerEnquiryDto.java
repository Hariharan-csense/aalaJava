package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record PartnerEnquiryDto(
    Object id,
    String name,
    String phoneNumber,
    String email,
    String city,
    String hotelName,
    String locationWithinCity,
    String locationPinCode,
    String propertyAge,
    String numberOfRooms,
    Object createdAt) {
  public static PartnerEnquiryDto from(Map<String, Object> row) {
    return new PartnerEnquiryDto(
        row.get("id"),
        optional(row.get("name"), ""),
        optional(row.get("phoneNumber"), ""),
        optional(row.get("email"), ""),
        optional(row.get("city"), ""),
        optional(row.get("hotelName"), ""),
        optional(row.get("locationWithinCity"), ""),
        optional(row.get("locationPinCode"), ""),
        optional(row.get("propertyAge"), ""),
        optional(row.get("numberOfRooms"), ""),
        row.get("createdAt"));
  }
}
