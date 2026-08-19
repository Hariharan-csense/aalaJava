package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record MembershipOrderDto(
    Integer id,
    Integer membershipPackageId,
    String packageName,
    Number amount,
    String currency,
    String name,
    String mobileNumber,
    String email,
    String location,
    String razorpayOrderId,
    String razorpayPaymentId,
    String receipt,
    String status,
    Object paymentDate,
    Object expiryDate,
    Object renewalDate,
    Object createdAt) {
  public static MembershipOrderDto from(Map<String, Object> row) {
    return new MembershipOrderDto(
        number(row.get("id"), 0).intValue(),
        number(row.get("membershipPackageId"), 0).intValue(),
        optional(row.get("packageName"), ""),
        number(row.get("amount"), 0),
        optional(row.get("currency"), ""),
        optional(row.get("name"), ""),
        optional(row.get("mobileNumber"), ""),
        optional(row.get("email"), ""),
        optional(row.get("location"), ""),
        optional(row.get("razorpayOrderId"), ""),
        optional(row.get("razorpayPaymentId"), ""),
        optional(row.get("receipt"), ""),
        optional(row.get("status"), ""),
        row.get("paymentDate"),
        row.get("expiryDate"),
        row.get("renewalDate"),
        row.get("createdAt"));
  }
}
