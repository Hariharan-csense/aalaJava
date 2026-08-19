package com.aalago.aalago.backend.repository;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import com.aalago.aalago.backend.exception.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
public class ContentRepository {
  private final JdbcTemplate jdbc;

  public ContentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> destinations() {
    return jdbc.query("""
        select d.id, d.name, d.state, d.image, d.description, count(p.id) properties
        from destinations d
        left join properties p on d.id = p.destination_id
        group by d.id, d.name, d.state, d.image, d.description, d.created_at
        order by d.created_at asc
        """, (rs, rowNum) -> mapOf(
        "id", rs.getString("id"),
        "name", rs.getString("name"),
        "state", rs.getString("state"),
        "image", rs.getString("image"),
        "description", rs.getString("description"),
        "properties", rs.getLong("properties")));
  }

  public Optional<Map<String, Object>> destination(String id) {
    List<Map<String, Object>> rows = jdbc.query("""
        select d.id, d.name, d.state, d.image, d.description, count(p.id) properties
        from destinations d left join properties p on d.id = p.destination_id
        where d.id = ?
        group by d.id, d.name, d.state, d.image, d.description
        """, (rs, rowNum) -> mapOf(
        "id", rs.getString("id"),
        "name", rs.getString("name"),
        "state", rs.getString("state"),
        "image", rs.getString("image"),
        "description", rs.getString("description"),
        "properties", rs.getLong("properties")), id);
    return rows.stream().findFirst();
  }

  @Transactional
  public Map<String, Object> createDestination(Map<String, Object> payload) {
    String id = required(payload.get("id"), "Destination id");
    try {
      jdbc.update("insert into destinations(id,name,state,image,description) values(?,?,?,?,?)",
          id,
          required(payload.get("name"), "Destination name"),
          required(payload.get("state"), "Destination state"),
          required(payload.get("image"), "Destination image"),
          required(payload.get("description"), "Destination description"));
    } catch (DuplicateKeyException ex) {
      throw new ApiException(HttpStatus.CONFLICT, "Destination id already exists");
    }
    return destination(id).orElseThrow();
  }

  @Transactional
  public Map<String, Object> updateDestination(String id, Map<String, Object> payload) {
    Map<String, Object> existing = destination(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Destination not found"));
    jdbc.update("""
        update destinations set name=?, state=?, image=?, description=?, updated_at=current_timestamp where id=?
        """,
        optional(payload.get("name"), existing.get("name")),
        optional(payload.get("state"), existing.get("state")),
        optional(payload.get("image"), existing.get("image")),
        optional(payload.get("description"), existing.get("description")),
        id);
    return destination(id).orElseThrow();
  }

  @Transactional
  public void deleteDestination(String id) {
    List<String> propertyIds = jdbc.queryForList("select id from properties where destination_id=?", String.class, id);
    for (String propertyId : propertyIds) {
      jdbc.update("delete from property_images where property_id=?", propertyId);
      jdbc.update("delete from property_amenities where property_id=?", propertyId);
      jdbc.update("delete from property_highlights where property_id=?", propertyId);
    }
    jdbc.update("delete from properties where destination_id=?", id);
    int deleted = jdbc.update("delete from destinations where id=?", id);
    if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "Destination not found");
  }

  public List<Map<String, Object>> properties(String destinationId) {
    String sql = "select * from properties " + (StringUtils.hasText(destinationId) ? "where destination_id=? " : "") + "order by created_at asc";
    List<Map<String, Object>> rows = StringUtils.hasText(destinationId) ? jdbc.queryForList(sql, destinationId) : jdbc.queryForList(sql);
    return rows.stream().map(this::propertyRow).toList();
  }

  public Optional<Map<String, Object>> property(String id) {
    List<Map<String, Object>> rows = jdbc.queryForList("select * from properties where id=?", id);
    return rows.stream().findFirst().map(this::propertyRow);
  }

  @Transactional
  public Map<String, Object> createProperty(Map<String, Object> payload) {
    String id = required(payload.get("id"), "Property id");
    String destinationId = required(payload.get("destinationId"), "Property destination");
    if (destination(destinationId).isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Destination not found");
    List<String> images = stringList(payload.get("images"));
    String image = optional(payload.get("image"), images.isEmpty() ? null : images.get(0));
    image = required(image, "Property main image");
    String bookingUrl = normalizeBookingUrl(optional(payload.get("bookingUrl"), ""));
    try {
      jdbc.update("""
          insert into properties(id,destination_id,name,location,type,price,rating,reviews,popular,image,description,booking_url,google_place_id,google_map_link,google_review_link,google_latitude,google_longitude,google_reviews_json,google_details_json)
          values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
          """,
          id, destinationId,
          required(payload.get("name"), "Property name"),
          required(payload.get("location"), "Property location"),
          required(payload.get("type"), "Property type"),
          number(payload.get("price"), 0),
          number(payload.get("rating"), 0),
          number(payload.get("reviews"), 0).intValue(),
          bool(payload.get("popular")),
          image,
          required(payload.get("description"), "Property description"),
          bookingUrl,
          optional(payload.get("googlePlaceId"), ""),
          optional(payload.get("googleMapLink"), ""),
          optional(payload.get("googleReviewLink"), ""),
          nullableNumber(payload.get("googleLatitude")),
          nullableNumber(payload.get("googleLongitude")),
          optional(payload.get("googleReviewsJson"), ""),
          optional(payload.get("googleDetailsJson"), ""));
    } catch (DuplicateKeyException ex) {
      throw new ApiException(HttpStatus.CONFLICT, "Property id already exists");
    }
    replacePropertyLists(id, images.isEmpty() ? List.of(image) : images, stringList(payload.get("amenities")), stringList(payload.get("highlights")));
    return property(id).orElseThrow();
  }

  @Transactional
  public Map<String, Object> updateProperty(String id, Map<String, Object> payload) {
    Map<String, Object> existing = property(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Property not found"));
    String destinationId = optional(payload.get("destinationId"), existing.get("destinationId"));
    if (destination(destinationId).isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Destination not found");
    List<String> images = payload.containsKey("images") ? stringList(payload.get("images")) : strings(existing.get("images"));
    String image = optional(payload.get("image"), images.isEmpty() ? existing.get("image") : images.get(0));
    image = required(image, "Property main image");
    String bookingUrl = normalizeBookingUrl(optional(payload.get("bookingUrl"), existing.get("bookingUrl")));
    jdbc.update("""
        update properties set destination_id=?, name=?, location=?, type=?, price=?, rating=?, reviews=?, popular=?, image=?, description=?, booking_url=?, google_place_id=?, google_map_link=?, google_review_link=?, google_latitude=?, google_longitude=?, google_reviews_json=?, google_details_json=?, updated_at=current_timestamp
        where id=?
        """,
        destinationId,
        optional(payload.get("name"), existing.get("name")),
        optional(payload.get("location"), existing.get("location")),
        optional(payload.get("type"), existing.get("type")),
        number(payload.get("price"), existing.get("price")),
        number(payload.get("rating"), existing.get("rating")),
        number(payload.get("reviews"), existing.get("reviews")).intValue(),
        payload.containsKey("popular") ? bool(payload.get("popular")) : bool(existing.get("popular")),
        image,
        optional(payload.get("description"), existing.get("description")),
        bookingUrl,
        optional(payload.get("googlePlaceId"), existing.get("googlePlaceId")),
        optional(payload.get("googleMapLink"), existing.get("googleMapLink")),
        optional(payload.get("googleReviewLink"), existing.get("googleReviewLink")),
        nullableNumber(payload.containsKey("googleLatitude") ? payload.get("googleLatitude") : existing.get("googleLatitude")),
        nullableNumber(payload.containsKey("googleLongitude") ? payload.get("googleLongitude") : existing.get("googleLongitude")),
        optional(payload.get("googleReviewsJson"), existing.get("googleReviewsJson")),
        optional(payload.get("googleDetailsJson"), existing.get("googleDetailsJson")),
        id);
    replacePropertyLists(id,
        images.isEmpty() ? List.of(image) : images,
        payload.containsKey("amenities") ? stringList(payload.get("amenities")) : strings(existing.get("amenities")),
        payload.containsKey("highlights") ? stringList(payload.get("highlights")) : strings(existing.get("highlights")));
    return property(id).orElseThrow();
  }

  @Transactional
  public void deleteProperty(String id) {
    jdbc.update("delete from property_images where property_id=?", id);
    jdbc.update("delete from property_amenities where property_id=?", id);
    jdbc.update("delete from property_highlights where property_id=?", id);
    int deleted = jdbc.update("delete from properties where id=?", id);
    if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "Property not found");
  }

  public Map<String, Object> createPartnerEnquiry(Map<String, Object> payload) {
    String phone = phone(payload.get("phoneNumber"));
    String email = email(payload.get("email"));
    jdbc.update("""
        insert into partner_enquiries(name,phone_number,email,city,hotel_name,location_within_city,location_pin_code,property_age,number_of_rooms,crm_payload)
        values(?,?,?,?,?,?,?,?,?,?)
        """,
        required(payload.get("name"), "Name"), phone, email,
        required(payload.get("city"), "City"),
        required(payload.get("hotelName"), "Hotel name"),
        optional(payload.get("locationWithinCity"), ""),
        required(payload.get("locationPinCode"), "Location pin code"),
        required(payload.get("propertyAge"), "Age of the property"),
        required(payload.get("numberOfRooms"), "Number of rooms"),
        payload.get("crmPayload") == null ? null : String.valueOf(payload.get("crmPayload")));
    Integer id = jdbc.queryForObject("select last_insert_id()", Integer.class);
    return partnerEnquiry(id);
  }

  public List<Map<String, Object>> partnerEnquiries() {
    return jdbc.query("select * from partner_enquiries order by created_at desc", (rs, i) -> mapOf(
        "id", rs.getInt("id"),
        "name", rs.getString("name"),
        "phoneNumber", rs.getString("phone_number"),
        "email", rs.getString("email"),
        "city", rs.getString("city"),
        "hotelName", rs.getString("hotel_name"),
        "locationWithinCity", rs.getString("location_within_city"),
        "locationPinCode", rs.getString("location_pin_code"),
        "propertyAge", rs.getString("property_age"),
        "numberOfRooms", rs.getString("number_of_rooms"),
        "createdAt", rs.getTimestamp("created_at")));
  }

  public Map<String, Object> createSubscriber(Map<String, Object> payload) {
    String email = email(payload.get("email"));
    List<Map<String, Object>> existing = jdbc.queryForList("select * from subscribers where email=?", email);
    if (!existing.isEmpty()) return subscriberRow(existing.get(0));
    jdbc.update("insert into subscribers(email,source) values(?,?)", email, optional(payload.get("source"), "Newsletter Banner"));
    Integer id = jdbc.queryForObject("select last_insert_id()", Integer.class);
    return subscriber(id);
  }

  public List<Map<String, Object>> subscribers() {
    return jdbc.queryForList("select * from subscribers order by created_at desc").stream().map(this::subscriberRow).toList();
  }

  private Map<String, Object> propertyRow(Map<String, Object> row) {
    String id = String.valueOf(row.get("id"));
    List<String> images = relation("property_images", "image", id);
    String image = String.valueOf(row.get("image"));
    if (images.isEmpty()) images = List.of(image);
    return mapOf(
        "id", id,
        "name", row.get("name"),
        "location", row.get("location"),
        "destinationId", row.get("destination_id"),
        "type", row.get("type"),
        "price", number(row.get("price"), 0),
        "rating", number(row.get("rating"), 0),
        "reviews", number(row.get("reviews"), 0),
        "popular", bool(row.get("popular")),
        "amenities", relation("property_amenities", "name", id),
        "image", image,
        "images", images,
        "description", row.get("description"),
        "highlights", relation("property_highlights", "text", id),
        "bookingUrl", safeBookingUrl(String.valueOf(row.getOrDefault("booking_url", ""))),
        "googlePlaceId", value(row, "google_place_id"),
        "googleMapLink", value(row, "google_map_link"),
        "googleReviewLink", value(row, "google_review_link"),
        "googleLatitude", value(row, "google_latitude"),
        "googleLongitude", value(row, "google_longitude"),
        "googleReviewsJson", value(row, "google_reviews_json"),
        "googleDetailsJson", value(row, "google_details_json"));
  }

  private static Object nullableNumber(Object value) {
    String text = optional(value, "");
    return StringUtils.hasText(text) ? number(text, 0) : null;
  }

  private List<String> relation(String table, String column, String propertyId) {
    return jdbc.queryForList("select " + column + " from " + table + " where property_id=? order by sort_order asc, id asc", String.class, propertyId);
  }

  private void replacePropertyLists(String id, List<String> images, List<String> amenities, List<String> highlights) {
    replace("property_images", "image", id, images);
    replace("property_amenities", "name", id, amenities);
    replace("property_highlights", "text", id, highlights);
  }

  private void replace(String table, String column, String propertyId, List<String> values) {
    jdbc.update("delete from " + table + " where property_id=?", propertyId);
    for (int i = 0; i < values.size(); i++) {
      jdbc.update("insert into " + table + "(property_id," + column + ",sort_order) values(?,?,?)", propertyId, values.get(i), i);
    }
  }

  private Map<String, Object> partnerEnquiry(Integer id) {
    return partnerEnquiries().stream().filter(row -> row.get("id").equals(id)).findFirst().orElseThrow();
  }

  private Map<String, Object> subscriber(Integer id) {
    return jdbc.queryForList("select * from subscribers where id=?", id).stream().findFirst().map(this::subscriberRow).orElseThrow();
  }

  private Map<String, Object> subscriberRow(Map<String, Object> row) {
    return mapOf("id", row.get("id"), "email", row.get("email"), "source", row.get("source"), "createdAt", row.get("created_at"));
  }

  private static String phone(Object value) {
    String phone = required(value, "Phone number");
    if (!phone.matches("[6-9]\\d{9}")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Phone number must be 10 digits and start with 6, 7, 8, or 9");
    }
    return phone;
  }
}
