package com.aalago.aalago.backend.entity;

import java.math.BigDecimal;
import java.util.List;

public record Property(
    String id,
    String name,
    String location,
    String destinationId,
    String type,
    BigDecimal price,
    BigDecimal rating,
    int reviews,
    boolean popular,
    List<String> amenities,
    String image,
    List<String> images,
    String description,
    List<String> highlights,
    String bookingUrl) {}
