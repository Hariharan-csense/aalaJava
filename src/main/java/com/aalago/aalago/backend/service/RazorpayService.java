package com.aalago.aalago.backend.service;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import com.aalago.aalago.backend.exception.ApiException;
import com.aalago.aalago.backend.repository.MembershipOrderRepository;
import com.aalago.aalago.backend.repository.MembershipRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RazorpayService {
  private final MembershipRepository memberships;
  private final MembershipOrderRepository orders;
  private final ObjectMapper mapper;
  private final HttpClient http;
  private final String keyId;
  private final String keySecret;

  public RazorpayService(
      MembershipRepository memberships,
      MembershipOrderRepository orders,
      ObjectMapper mapper,
      @Value("${razorpay.key.id}") String keyId,
      @Value("${razorpay.key.secret}") String keySecret) {
    this.memberships = memberships;
    this.orders = orders;
    this.mapper = mapper;
    this.http = HttpClient.newHttpClient();
    this.keyId = keyId;
    this.keySecret = keySecret;
  }

  public Map<String, Object> createMembershipOrder(Map<String, Object> payload) {
    requireKeys();
    Integer packageId = number(payload.get("membershipPackageId"), 0).intValue();
    Map<String, Object> plan = memberships.packageById(packageId);
    String receipt = "mem_" + System.currentTimeMillis();
    int amountPaise = number(plan.get("price"), 0).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    if (amountPaise < 1000) throw new ApiException(HttpStatus.BAD_REQUEST, "Membership amount must be at least Rs.10");

    Map<String, Object> body = mapOf(
        "amount", amountPaise,
        "currency", "INR",
        "receipt", receipt,
        "notes", mapOf(
            "membershipPackageId", String.valueOf(packageId),
            "packageName", optional(plan.get("name"), ""),
            "customerName", required(payload.get("name"), "Name"),
            "mobileNumber", required(payload.get("mobileNumber"), "Mobile number")));

    JsonNode razorpayOrder = createOrder(body);
    Map<String, Object> order = orders.create(payload, plan, razorpayOrder.path("id").asText(), receipt);
    return mapOf(
        "order", order,
        "razorpay", mapOf(
            "keyId", keyId,
            "orderId", razorpayOrder.path("id").asText(),
            "amount", amountPaise,
            "currency", "INR",
            "name", "AalaGO Membership",
            "description", plan.get("name")));
  }

  public Map<String, Object> verifyMembershipPayment(Map<String, Object> payload) {
    requireKeys();
    String razorpayOrderId = required(payload.get("razorpay_order_id"), "Razorpay order id");
    String paymentId = required(payload.get("razorpay_payment_id"), "Razorpay payment id");
    String signature = required(payload.get("razorpay_signature"), "Razorpay signature");
    Map<String, Object> order = orders.orderByRazorpayId(razorpayOrderId);
    String storedOrderId = optional(order.get("razorpayOrderId"), "");
    if (!verifySignature(storedOrderId, paymentId, signature)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Payment signature verification failed");
    }
    return orders.markPaid(storedOrderId, paymentId, signature);
  }

  private JsonNode createOrder(Map<String, Object> body) {
    try {
      String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
      HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/orders"))
          .header("Authorization", "Basic " + auth)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
          .build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay order creation failed");
      }
      return mapper.readTree(response.body());
    } catch (ApiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay order creation failed");
    }
  }

  private boolean verifySignature(String orderId, String paymentId, String signature) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
      String expected = hex(digest);
      return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      return false;
    }
  }

  private void requireKeys() {
    if (!StringUtils.hasText(keyId) || !StringUtils.hasText(keySecret)) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Razorpay keys are missing");
    }
  }

  private static String hex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) builder.append(String.format("%02x", value));
    return builder.toString();
  }
}
