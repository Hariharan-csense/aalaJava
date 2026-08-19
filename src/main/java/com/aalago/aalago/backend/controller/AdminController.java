package com.aalago.aalago.backend.controller;

import com.aalago.aalago.backend.dto.ApiResponse;
import com.aalago.aalago.backend.dto.AdminLoginDto;
import com.aalago.aalago.backend.dto.BlogPostDto;
import com.aalago.aalago.backend.dto.DestinationDto;
import com.aalago.aalago.backend.dto.PartnerEnquiryDto;
import com.aalago.aalago.backend.dto.PropertyDto;
import com.aalago.aalago.backend.dto.SubscriberDto;
import com.aalago.aalago.backend.dto.UploadDto;
import com.aalago.aalago.backend.service.AuthService;
import com.aalago.aalago.backend.service.ContentService;
import com.aalago.aalago.backend.service.StoreService;
import com.aalago.aalago.backend.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final AuthService auth;
  private final ContentService content;
  private final StoreService store;
  private final UploadService uploads;

  public AdminController(AuthService auth, ContentService content, StoreService store, UploadService uploads) {
    this.auth = auth;
    this.content = content;
    this.store = store;
    this.uploads = uploads;
  }

  @PostMapping("/login")
  AdminLoginDto login(@RequestBody Map<String, Object> payload) {
    return AdminLoginDto.from(auth.login(text(payload.get("email")), text(payload.get("password"))));
  }

  @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ApiResponse<UploadDto>> upload(HttpServletRequest request, @RequestPart("image") MultipartFile image) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(UploadDto.from(uploads.save(request, image))));
  }

  @GetMapping({"/destinations", "/destination"})
  ApiResponse<List<DestinationDto>> destinations() {
    return new ApiResponse<>(content.destinations().stream().map(DestinationDto::from).toList());
  }

  @PostMapping("/destinations")
  ResponseEntity<ApiResponse<DestinationDto>> createDestination(@RequestBody Map<String, Object> payload) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(DestinationDto.from(content.createDestination(payload))));
  }

  @PutMapping("/destinations/{id}")
  ApiResponse<DestinationDto> updateDestination(@PathVariable String id, @RequestBody Map<String, Object> payload) {
    return new ApiResponse<>(DestinationDto.from(content.updateDestination(id, payload)));
  }

  @DeleteMapping("/destinations/{id}")
  ResponseEntity<Void> deleteDestination(@PathVariable String id) {
    content.deleteDestination(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/properties")
  ApiResponse<List<PropertyDto>> properties(@RequestParam(required = false) String destinationId) {
    return new ApiResponse<>(content.properties(destinationId).stream().map(PropertyDto::from).toList());
  }

  @PostMapping("/properties")
  ResponseEntity<ApiResponse<PropertyDto>> createProperty(@RequestBody Map<String, Object> payload) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(PropertyDto.from(content.createProperty(payload))));
  }

  @PutMapping("/properties/{id}")
  ApiResponse<PropertyDto> updateProperty(@PathVariable String id, @RequestBody Map<String, Object> payload) {
    return new ApiResponse<>(PropertyDto.from(content.updateProperty(id, payload)));
  }

  @DeleteMapping("/properties/{id}")
  ResponseEntity<Void> deleteProperty(@PathVariable String id) {
    content.deleteProperty(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/blog-posts")
  ApiResponse<List<BlogPostDto>> blogPosts() {
    return new ApiResponse<>(store.blogPosts().stream().map(BlogPostDto::from).toList());
  }

  @PostMapping("/blog-posts")
  ResponseEntity<ApiResponse<BlogPostDto>> createBlogPost(@RequestBody Map<String, Object> payload) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(BlogPostDto.from(store.createBlogPost(payload))));
  }

  @PutMapping("/blog-posts/{id}")
  ApiResponse<BlogPostDto> updateBlogPost(@PathVariable String id, @RequestBody Map<String, Object> payload) {
    return new ApiResponse<>(BlogPostDto.from(store.updateBlogPost(id, payload)));
  }

  @DeleteMapping("/blog-posts/{id}")
  ResponseEntity<Void> deleteBlogPost(@PathVariable String id) {
    store.deleteBlogPost(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/page-content/{slug}")
  ApiResponse<Map<String, Object>> getPageContent(@PathVariable String slug) {
    return new ApiResponse<>(store.pageContent(slug));
  }

  @PutMapping("/page-content/{slug}")
  ApiResponse<Map<String, Object>> updatePageContent(@PathVariable String slug, @RequestBody Map<String, Object> payload) {
    return new ApiResponse<>(store.updatePageContent(slug, payload));
  }

  @GetMapping("/partner-enquiries")
  ApiResponse<List<PartnerEnquiryDto>> partnerEnquiries() {
    return new ApiResponse<>(content.partnerEnquiries().stream().map(PartnerEnquiryDto::from).toList());
  }

  @GetMapping("/subscribers")
  ApiResponse<List<SubscriberDto>> subscribers() {
    return new ApiResponse<>(content.subscribers().stream().map(SubscriberDto::from).toList());
  }

 
  

  private static String text(Object value) {
    return value instanceof String s ? s : "";
  }
}
