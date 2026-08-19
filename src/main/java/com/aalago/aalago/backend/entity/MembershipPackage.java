package com.aalago.aalago.backend.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MembershipPackage(
    Integer id,
    String name,
    BigDecimal price,
    String period,
    List<String> features,
    Map<String, Object> benefits,
    boolean popular,
    BigDecimal sortOrder) {}
