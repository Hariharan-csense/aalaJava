package com.aalago.aalago.backend.controller;

import com.aalago.aalago.backend.dto.ApiResponse;
import com.aalago.aalago.backend.dto.BlogPostDto;
import com.aalago.aalago.backend.dto.DestinationDto;
import com.aalago.aalago.backend.dto.MembershipOrderCreateDto;
import com.aalago.aalago.backend.dto.MembershipOrderDto;
import com.aalago.aalago.backend.dto.MembershipPackageDto;
import com.aalago.aalago.backend.dto.PartnerEnquiryDto;
import com.aalago.aalago.backend.dto.PropertyDto;
import com.aalago.aalago.backend.dto.SubscriberDto;
import com.aalago.aalago.backend.exception.ApiException;
import com.aalago.aalago.backend.service.ContentService;
import com.aalago.aalago.backend.service.MembershipService;
import com.aalago.aalago.backend.service.RazorpayService;
import com.aalago.aalago.backend.service.StoreService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PublicController {
  private final ContentService content;
  private final StoreService store;
  private final MembershipService memberships;
  private final RazorpayService razorpay;

  public PublicController(ContentService content, StoreService store, MembershipService memberships, RazorpayService razorpay) {
    this.content = content;
    this.store = store;
    this.memberships = memberships;
    this.razorpay = razorpay;
  }


  @GetMapping({"/destinations", "/destination"})                                     
  ApiResponse<List<DestinationDto>> destinations() {
    return new ApiResponse<>(content.destinations().stream().map(DestinationDto::from).toList());
  }

  @GetMapping("/properties")
  ApiResponse<List<PropertyDto>> properties(@RequestParam(required = false) String destinationId) {
    return new ApiResponse<>(content.properties(destinationId).stream().map(PropertyDto::from).toList());
  }

  @GetMapping("/properties/{id}")
  ApiResponse<PropertyDto> property(@PathVariable String id) {
    return new ApiResponse<>(PropertyDto.from(content.property(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Property not found"))));
  }

  @GetMapping("/blog-posts")
  ApiResponse<List<BlogPostDto>> blogPosts() {
    return new ApiResponse<>(store.blogPosts().stream().map(BlogPostDto::from).toList());
  }

  @GetMapping("/page-content/{slug}")
  ApiResponse<Map<String, Object>> pageContent(@PathVariable String slug) {
    return new ApiResponse<>(store.pageContent(slug));
  }

  @PostMapping("/partner-enquiries")
  ResponseEntity<ApiResponse<PartnerEnquiryDto>> partnerEnquiry(@RequestBody Map<String, Object> payload) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(PartnerEnquiryDto.from(content.createPartnerEnquiry(payload))));
  }

  @GetMapping("/memberships")
  ApiResponse<List<MembershipPackageDto>> memberships() {
    return new ApiResponse<>(memberships.packages().stream().map(MembershipPackageDto::from).toList());
  }

  @PostMapping("/membership-orders")
  ResponseEntity<ApiResponse<MembershipOrderCreateDto>> membershipOrder(@RequestBody Map<String, Object> payload) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(MembershipOrderCreateDto.from(razorpay.createMembershipOrder(payload))));
  }

  @PostMapping("/membership-orders/verify")
  ApiResponse<MembershipOrderDto> verifyMembershipOrder(@RequestBody Map<String, Object> payload) {
    return new ApiResponse<>(MembershipOrderDto.from(razorpay.verifyMembershipPayment(payload)));
  }

  @PostMapping("/subscribers")
  ResponseEntity<ApiResponse<SubscriberDto>> subscriber(@RequestBody Map<String, Object> payload) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(SubscriberDto.from(content.createSubscriber(payload))));
  }
}
