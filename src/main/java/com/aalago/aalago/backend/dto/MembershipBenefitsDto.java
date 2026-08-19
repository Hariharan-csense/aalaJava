package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record MembershipBenefitsDto(
    String bookingDiscount,
    String rewardWallet,
    String complimentaryBreakfast,
    String earlyCheckInLateCheckOut,
    String priorityBooking,
    String memberOnlyDeals,
    String travelWelcomeKit,
    String priorityCustomerSupport) {
  public static MembershipBenefitsDto from(Map<String, Object> row) {
    return new MembershipBenefitsDto(
        optional(row.get("bookingDiscount"), ""),
        optional(row.get("rewardWallet"), ""),
        optional(row.get("complimentaryBreakfast"), ""),
        optional(row.get("earlyCheckInLateCheckOut"), ""),
        optional(row.get("priorityBooking"), ""),
        optional(row.get("memberOnlyDeals"), ""),
        optional(row.get("travelWelcomeKit"), ""),
        optional(row.get("priorityCustomerSupport"), ""));
  }
}
