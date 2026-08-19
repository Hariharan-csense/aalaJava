package com.aalago.aalago.backend.repository;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import com.aalago.aalago.backend.exception.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipOrderRepository {
  private final JdbcTemplate jdbc;

  public MembershipOrderRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> orders() {
    return jdbc.queryForList("select * from membership_orders where status='paid' order by payment_date desc, id desc").stream()
        .map(this::row)
        .toList();
  }

  public Map<String, Object> create(Map<String, Object> customer, Map<String, Object> plan, String razorpayOrderId, String receipt) {
    jdbc.update("""
        insert into membership_orders(membership_package_id,package_name,amount,currency,name,mobile_number,email,location,razorpay_order_id,receipt,status)
        values(?,?,?,?,?,?,?,?,?,?,?)
        """,
        number(plan.get("id"), 0).intValue(),
        plan.get("name"),
        number(plan.get("price"), 0),
        "INR",
        required(customer.get("name"), "Name"),
        required(customer.get("mobileNumber"), "Mobile number"),
        optional(customer.get("email"), ""),
        required(customer.get("location"), "Location"),
        razorpayOrderId,
        receipt,
        "created");
    Integer id = jdbc.queryForObject("select last_insert_id()", Integer.class);
    return orderById(id);
  }

  public Map<String, Object> markPaid(String razorpayOrderId, String paymentId, String signature) {
    int updated = jdbc.update("""
        update membership_orders
        set status='paid', razorpay_payment_id=?, razorpay_signature=?, payment_date=current_timestamp,
            expiry_date=date_add(current_timestamp, interval 1 year),
            renewal_date=date_add(current_timestamp, interval 1 year),
            updated_at=current_timestamp
        where razorpay_order_id=?
        """, paymentId, signature, razorpayOrderId);
    if (updated == 0) throw new ApiException(HttpStatus.NOT_FOUND, "Membership order not found");
    return orderByRazorpayId(razorpayOrderId);
  }

  public Map<String, Object> orderByRazorpayId(String razorpayOrderId) {
    return jdbc.queryForList("select * from membership_orders where razorpay_order_id=?", razorpayOrderId).stream()
        .findFirst()
        .map(this::row)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership order not found"));
  }

  private Map<String, Object> orderById(Integer id) {
    return jdbc.queryForList("select * from membership_orders where id=?", id).stream()
        .findFirst()
        .map(this::row)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership order not found"));
  }

  private Map<String, Object> row(Map<String, Object> row) {
    BigDecimal amount = number(row.get("amount"), 0);
    return mapOf(
        "id", number(row.get("id"), 0).intValue(),
        "membershipPackageId", number(row.get("membership_package_id"), 0).intValue(),
        "packageName", value(row, "package_name"),
        "amount", amount,
        "currency", value(row, "currency"),
        "name", value(row, "name"),
        "mobileNumber", value(row, "mobile_number"),
        "email", value(row, "email"),
        "location", value(row, "location"),
        "razorpayOrderId", value(row, "razorpay_order_id"),
        "razorpayPaymentId", value(row, "razorpay_payment_id"),
        "receipt", value(row, "receipt"),
        "status", value(row, "status"),
        "paymentDate", value(row, "payment_date"),
        "expiryDate", value(row, "expiry_date"),
        "renewalDate", value(row, "renewal_date"),
        "createdAt", value(row, "created_at"));
  }
}
