package com.aalago.aalago.backend.entity;

public record PartnerEnquiry(
    Integer id,
    String name,
    String phoneNumber,
    String email,
    String city,
    String hotelName,
    String locationWithinCity,
    String locationPinCode,
    String propertyAge,
    String numberOfRooms,
    Object createdAt) {}
