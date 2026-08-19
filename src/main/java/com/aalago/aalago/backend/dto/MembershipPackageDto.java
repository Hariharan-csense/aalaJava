package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;
import java.util.List;

public record MembershipPackageDto(
    Integer id,
    String name,
    Number price,
    String period,
    List<String> features,
    MembershipBenefitsDto benefits,
    boolean popular,
    Number sortOrder) {
  public static MembershipPackageDto from(Map<String, Object> row) {
    return new MembershipPackageDto(
        number(row.get("id"), 0).intValue(),
        optional(row.get("name"), ""),
        number(row.get("price"), 0),
        optional(row.get("period"), ""),
        stringList(row.get("features")),
        MembershipBenefitsDto.from(map(row.get("benefits"))),
        bool(row.get("popular")),
        number(row.get("sortOrder"), 0));
  }
}
