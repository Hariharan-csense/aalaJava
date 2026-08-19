package com.aalago.aalago.backend.entity;

public record BlogPost(
    String id,
    String title,
    String excerpt,
    String author,
    String readTime,
    String category,
    String date,
    String image) {}
