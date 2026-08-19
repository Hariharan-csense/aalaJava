package com.aalago.aalago.backend.dto;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import java.util.Map;

public record MembershipOrderCreateDto(
    MembershipOrderDto order,
    RazorpayCheckoutDto razorpay) {
  public static MembershipOrderCreateDto from(Map<String, Object> row) {
    return new MembershipOrderCreateDto(
        MembershipOrderDto.from(map(row.get("order"))),
        RazorpayCheckoutDto.from(map(row.get("razorpay"))));
  }
}
