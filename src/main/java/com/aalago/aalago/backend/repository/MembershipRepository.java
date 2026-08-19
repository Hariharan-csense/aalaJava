package com.aalago.aalago.backend.repository;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import com.aalago.aalago.backend.exception.ApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
public class MembershipRepository {
  private final JdbcTemplate jdbc;

  public MembershipRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> packages() {
    List<Map<String, Object>> rows = jdbc.queryForList("select * from membership_packages order by sort_order asc, id asc");
    return rows.stream().map(this::packageRow).toList();
  }

  @Transactional
  public Map<String, Object> create(Map<String, Object> payload) {
    Map<String, Object> item = normalize(payload, null);
    jdbc.update("""
        insert into membership_packages(name,price,period,booking_discount,reward_wallet,complimentary_breakfast,early_check_in_late_check_out,priority_booking,member_only_deals,travel_welcome_kit,priority_customer_support,popular,sort_order)
        values(?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, item.get("name"), item.get("price"), item.get("period"),
        benefits(item).get("bookingDiscount"), benefits(item).get("rewardWallet"), benefits(item).get("complimentaryBreakfast"),
        benefits(item).get("earlyCheckInLateCheckOut"), benefits(item).get("priorityBooking"), benefits(item).get("memberOnlyDeals"),
        benefits(item).get("travelWelcomeKit"), benefits(item).get("priorityCustomerSupport"), item.get("popular"), item.get("sortOrder"));
    Integer id = jdbc.queryForObject("select last_insert_id()", Integer.class);
    replaceFeatures(id, strings(item.get("features")));
    return packageById(id);
  }

  @Transactional
  public Map<String, Object> update(Integer id, Map<String, Object> payload) {
    Map<String, Object> existing = packageById(id);
    Map<String, Object> item = normalize(payload, existing);
    jdbc.update("""
        update membership_packages set name=?, price=?, period=?, booking_discount=?, reward_wallet=?, complimentary_breakfast=?,
        early_check_in_late_check_out=?, priority_booking=?, member_only_deals=?, travel_welcome_kit=?, priority_customer_support=?,
        popular=?, sort_order=?, updated_at=current_timestamp where id=?
        """, item.get("name"), item.get("price"), item.get("period"),
        benefits(item).get("bookingDiscount"), benefits(item).get("rewardWallet"), benefits(item).get("complimentaryBreakfast"),
        benefits(item).get("earlyCheckInLateCheckOut"), benefits(item).get("priorityBooking"), benefits(item).get("memberOnlyDeals"),
        benefits(item).get("travelWelcomeKit"), benefits(item).get("priorityCustomerSupport"), item.get("popular"), item.get("sortOrder"), id);
    replaceFeatures(id, strings(item.get("features")));
    return packageById(id);
  }

  @Transactional
  public void delete(Integer id) {
    jdbc.update("delete from membership_package_features where membership_package_id=?", id);
    int deleted = jdbc.update("delete from membership_packages where id=?", id);
    if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "Membership package not found");
  }

  public Map<String, Object> packageById(Integer id) {
    return jdbc.queryForList("select * from membership_packages where id=?", id).stream().findFirst()
        .map(this::packageRow)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership package not found"));
  }

  private Map<String, Object> packageRow(Map<String, Object> row) {
    Map<String, Object> benefits = mapOf(
        "bookingDiscount", value(row, "booking_discount"),
        "rewardWallet", value(row, "reward_wallet"),
        "complimentaryBreakfast", value(row, "complimentary_breakfast"),
        "earlyCheckInLateCheckOut", value(row, "early_check_in_late_check_out"),
        "priorityBooking", value(row, "priority_booking"),
        "memberOnlyDeals", value(row, "member_only_deals"),
        "travelWelcomeKit", value(row, "travel_welcome_kit"),
        "priorityCustomerSupport", value(row, "priority_customer_support"));
    Integer id = number(row.get("id"), 0).intValue();
    return mapOf(
        "id", id,
        "name", row.get("name"),
        "price", number(row.get("price"), 0),
        "period", row.get("period"),
        "features", features(id),
        "benefits", benefits,
        "popular", bool(row.get("popular")),
        "sortOrder", number(row.get("sort_order"), 0));
  }

  private Map<String, Object> normalize(Map<String, Object> payload, Map<String, Object> existing) {
    Map<String, Object> benefits = payload.containsKey("benefits")
        ? normalizeBenefits(payload.get("benefits"), existing == null ? Map.of() : benefits(existing))
        : existing == null ? Map.of() : benefits(existing);
    List<String> features = payload.containsKey("features")
        ? stringList(payload.get("features"))
        : existing == null ? featuresFromBenefits(benefits) : strings(existing.get("features"));
    if (features.isEmpty()) features = featuresFromBenefits(benefits);
    return mapOf(
        "name", existing == null ? required(payload.get("name"), "Package name") : optional(payload.get("name"), existing.get("name")),
        "price", payload.containsKey("price") ? number(payload.get("price"), 0) : existing == null ? BigDecimal.ZERO : existing.get("price"),
        "period", existing == null ? required(payload.get("period"), "Package period") : optional(payload.get("period"), existing.get("period")),
        "features", features,
        "benefits", benefits,
        "popular", payload.containsKey("popular") ? bool(payload.get("popular")) : existing != null && bool(existing.get("popular")),
        "sortOrder", payload.containsKey("sortOrder") ? number(payload.get("sortOrder"), 0) : existing == null ? 0 : existing.get("sortOrder"));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> normalizeBenefits(Object raw, Map<String, Object> fallback) {
    Map<String, Object> values = raw instanceof Map<?, ?> map ? new LinkedHashMap<>((Map<String, Object>) map) : new LinkedHashMap<>();
    Map<String, Object> result = new LinkedHashMap<>();
    for (String key : List.of("bookingDiscount", "rewardWallet", "complimentaryBreakfast", "earlyCheckInLateCheckOut", "priorityBooking", "memberOnlyDeals", "travelWelcomeKit", "priorityCustomerSupport")) {
      result.put(key, optional(values.get(key), fallback.getOrDefault(key, "")));
    }
    return result;
  }

  private List<String> features(Integer id) {
    return jdbc.queryForList("select text from membership_package_features where membership_package_id=? order by sort_order asc, id asc", String.class, id);
  }

  public void replaceFeatures(Integer id, List<String> features) {
    jdbc.update("delete from membership_package_features where membership_package_id=?", id);
    for (int i = 0; i < features.size(); i++) {
      jdbc.update("insert into membership_package_features(membership_package_id,text,sort_order) values(?,?,?)", id, features.get(i), i);
    }
  }

  public static List<String> featuresFromBenefits(Map<String, Object> benefits) {
    List<String> values = new ArrayList<>();
    add(values, benefits.get("bookingDiscount"));
    add(values, optional(benefits.get("rewardWallet"), "") + (StringUtils.hasText(optional(benefits.get("rewardWallet"), "")) ? " Reward Wallet" : ""));
    add(values, optional(benefits.get("complimentaryBreakfast"), "") + (StringUtils.hasText(optional(benefits.get("complimentaryBreakfast"), "")) ? " Complimentary Breakfast" : ""));
    add(values, optional(benefits.get("earlyCheckInLateCheckOut"), "") + (StringUtils.hasText(optional(benefits.get("earlyCheckInLateCheckOut"), "")) ? " Early Check-in / Late Check-out" : ""));
    if (truthy(benefits.get("priorityBooking"))) values.add("Priority Booking");
    if (truthy(benefits.get("memberOnlyDeals"))) values.add("Member-Only Deals");
    String kit = optional(benefits.get("travelWelcomeKit"), "");
    if (StringUtils.hasText(kit) && !"No".equalsIgnoreCase(kit)) values.add("Travel Welcome Kit: " + kit);
    String support = optional(benefits.get("priorityCustomerSupport"), "");
    if (StringUtils.hasText(support) && !"No".equalsIgnoreCase(support)) values.add("Priority Customer Support: " + support);
    return values;
  }

  private static void add(List<String> values, Object value) {
    String text = optional(value, "");
    if (StringUtils.hasText(text)) values.add(text);
  }

  private static boolean truthy(Object value) {
    String text = optional(value, "").toLowerCase();
    return text.equals("yes") || text.equals("true") || text.equals("included");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> benefits(Map<String, Object> item) {
    return (Map<String, Object>) item.get("benefits");
  }
}
