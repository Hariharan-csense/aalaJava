package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.List;
import java.util.Map;

public record PropertyDto(
    String id,
    String name,
    String location,
    String destinationId,
    String type,
    Number price,
    Number rating,
    Number reviews,
    boolean popular,
    List<String> amenities,
    String image,
    List<String> images,
    String description,
    List<String> highlights,
    String bookingUrl,
    String googlePlaceId,
    String googleMapLink,
    String googleReviewLink,
    Object googleLatitude,
    Object googleLongitude,
    String googleReviewsJson,
    String googleDetailsJson) {
  public static PropertyDto from(Map<String, Object> row) {
    return new PropertyDto(
        optional(row.get("id"), ""),
        optional(row.get("name"), ""),
        optional(row.get("location"), ""),
        optional(row.get("destinationId"), ""),
        optional(row.get("type"), ""),
        number(row.get("price"), 0),
        number(row.get("rating"), 0),
        number(row.get("reviews"), 0),
        bool(row.get("popular")),
        stringList(row.get("amenities")),
        optional(row.get("image"), ""),
        stringList(row.get("images")),
        optional(row.get("description"), ""),
        stringList(row.get("highlights")),
        optional(row.get("bookingUrl"), ""),
        optional(row.get("googlePlaceId"), ""),
        optional(row.get("googleMapLink"), ""),
        optional(row.get("googleReviewLink"), ""),
        row.get("googleLatitude"),
        row.get("googleLongitude"),
        optional(row.get("googleReviewsJson"), ""),
        optional(row.get("googleDetailsJson"), ""));
  }
}
