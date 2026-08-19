package com.aalago.aalago.backend.service;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import com.aalago.aalago.backend.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GooglePlacesService {
  private final ObjectMapper mapper;
  private final HttpClient http;
  private final String apiKey;

  public GooglePlacesService(ObjectMapper mapper, @Value("${google.places.api.key}") String apiKey) {
    this.mapper = mapper;
    this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    this.apiKey = apiKey;
  }

  public Map<String, Object> enrichProperty(Map<String, Object> payload) {
    Map<String, Object> enriched = new LinkedHashMap<>(payload);
    boolean hasGoogleInput = StringUtils.hasText(optional(payload.get("googlePlaceId"), ""))
        || StringUtils.hasText(optional(payload.get("googleMapLink"), ""))
        || StringUtils.hasText(optional(payload.get("googleReviewLink"), ""));
    if (!hasGoogleInput) return enriched;
    if (!StringUtils.hasText(apiKey)) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Google Places API key is missing");

    try {
      String placeId = optional(payload.get("googlePlaceId"), "");
      if (!StringUtils.hasText(placeId)) {
        placeId = findPlaceId(payload);
      }
      if (!StringUtils.hasText(placeId)) return enriched;

      JsonNode place = details(placeId);
      enriched.put("googlePlaceId", place.path("id").asText(placeId));
      enriched.put("location", text(place.path("formattedAddress"), optional(payload.get("location"), "")));
      enriched.put("rating", place.path("rating").asDouble(number(payload.get("rating"), 0).doubleValue()));
      enriched.put("reviews", place.path("userRatingCount").asInt(number(payload.get("reviews"), 0).intValue()));
      enriched.put("googleMapLink", text(place.path("googleMapsUri"), optional(payload.get("googleMapLink"), "")));
      JsonNode links = place.path("googleMapsLinks");
      enriched.put("googleReviewLink", text(links.path("writeAReviewUri"), optional(payload.get("googleReviewLink"), "")));
      if (place.path("location").has("latitude")) enriched.put("googleLatitude", place.path("location").path("latitude").asText());
      if (place.path("location").has("longitude")) enriched.put("googleLongitude", place.path("location").path("longitude").asText());
      enriched.put("googleReviewsJson", mapper.writeValueAsString(reviewList(place.path("reviews"))));
      enriched.put("googleDetailsJson", mapper.writeValueAsString(detailMap(place)));
      return enriched;
    } catch (ApiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Google place details fetch failed");
    }
  }

  private String findPlaceId(Map<String, Object> payload) throws Exception {
    List<String> queries = List.of(
        List.of(optional(payload.get("name"), ""), optional(payload.get("location"), "")).stream()
            .filter(StringUtils::hasText)
            .reduce((a, b) -> a + ", " + b)
            .orElse(""),
        optional(payload.get("name"), ""),
        optional(payload.get("googleMapLink"), ""),
        optional(payload.get("googleReviewLink"), ""));
    for (String query : queries) {
      String placeId = searchPlaceId(query);
      if (StringUtils.hasText(placeId)) return placeId;
    }
    return "";
  }

  private String searchPlaceId(String query) throws Exception {
    if (!StringUtils.hasText(query)) return "";
    HttpRequest request = HttpRequest.newBuilder(URI.create("https://places.googleapis.com/v1/places:searchText"))
        .header("Content-Type", "application/json")
        .header("X-Goog-Api-Key", apiKey)
        .header("X-Goog-FieldMask", "places.id")
        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(mapOf("textQuery", query))))
        .build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) return legacySearchPlaceId(query);
    return mapper.readTree(response.body()).path("places").path(0).path("id").asText("");
  }

  private JsonNode details(String placeId) throws Exception {
    String fields = "id,displayName,formattedAddress,location,rating,userRatingCount,googleMapsUri,googleMapsLinks,reviews,businessStatus,nationalPhoneNumber,internationalPhoneNumber,websiteUri,priceLevel,types,editorialSummary,regularOpeningHours";
    HttpRequest request = HttpRequest.newBuilder(URI.create("https://places.googleapis.com/v1/places/" + URLEncoder.encode(placeId, StandardCharsets.UTF_8)))
        .header("Content-Type", "application/json")
        .header("X-Goog-Api-Key", apiKey)
        .header("X-Goog-FieldMask", fields)
        .GET()
        .build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      return legacyDetails(placeId);
    }
    return mapper.readTree(response.body());
  }

  private String legacySearchPlaceId(String query) throws Exception {
    String url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json"
        + "?input=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
        + "&inputtype=textquery&fields=place_id&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
    HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) return "";
    JsonNode root = mapper.readTree(response.body());
    if (!"OK".equals(root.path("status").asText())) return "";
    return root.path("candidates").path(0).path("place_id").asText("");
  }

  private JsonNode legacyDetails(String placeId) throws Exception {
    String url = "https://maps.googleapis.com/maps/api/place/details/json"
        + "?place_id=" + URLEncoder.encode(placeId, StandardCharsets.UTF_8)
        + "&fields=place_id,formatted_address,geometry,rating,user_ratings_total,url,reviews"
        + "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
    HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Google place details fetch failed");
    }
    JsonNode result = mapper.readTree(response.body()).path("result");
    if (result.isMissingNode()) throw new ApiException(HttpStatus.BAD_GATEWAY, "Google place details fetch failed");
    return mapper.valueToTree(mapOf(
        "id", result.path("place_id").asText(placeId),
        "formattedAddress", result.path("formatted_address").asText(""),
        "location", mapOf(
            "latitude", result.path("geometry").path("location").path("lat").asText(""),
            "longitude", result.path("geometry").path("location").path("lng").asText("")),
        "rating", result.path("rating").asDouble(0),
        "userRatingCount", result.path("user_ratings_total").asInt(0),
        "googleMapsUri", result.path("url").asText(""),
        "googleMapsLinks", mapOf("writeAReviewUri", ""),
        "reviews", legacyReviewList(result.path("reviews")),
        "details", mapOf(
            "businessStatus", result.path("business_status").asText(""),
            "phoneNumber", result.path("formatted_phone_number").asText(""),
            "website", result.path("website").asText(""),
            "priceLevel", result.path("price_level").asText(""),
            "types", stringArray(result.path("types")),
            "editorialSummary", result.path("editorial_summary").path("overview").asText(""),
            "openingHours", stringArray(result.path("opening_hours").path("weekday_text")))));
  }

  private List<Map<String, Object>> legacyReviewList(JsonNode reviews) {
    List<Map<String, Object>> values = new ArrayList<>();
    if (!reviews.isArray()) return values;
    for (JsonNode review : reviews) {
      values.add(mapOf(
          "authorAttribution", mapOf("displayName", review.path("author_name").asText("")),
          "rating", review.path("rating").asDouble(0),
          "text", mapOf("text", review.path("text").asText("")),
          "publishTime", review.path("time").asText(""),
          "googleMapsUri", ""));
    }
    return values;
  }

  private Map<String, Object> detailMap(JsonNode place) {
    JsonNode details = place.path("details");
    if (!details.isMissingNode()) {
      return mapOf(
          "businessStatus", details.path("businessStatus").asText(""),
          "phoneNumber", details.path("phoneNumber").asText(""),
          "website", details.path("website").asText(""),
          "priceLevel", details.path("priceLevel").asText(""),
          "types", stringArray(details.path("types")),
          "editorialSummary", details.path("editorialSummary").asText(""),
          "openingHours", stringArray(details.path("openingHours")));
    }
    return mapOf(
        "businessStatus", place.path("businessStatus").asText(""),
        "phoneNumber", place.path("nationalPhoneNumber").asText(place.path("internationalPhoneNumber").asText("")),
        "website", place.path("websiteUri").asText(""),
        "priceLevel", place.path("priceLevel").asText(""),
        "types", stringArray(place.path("types")),
        "editorialSummary", place.path("editorialSummary").path("text").asText(""),
        "openingHours", stringArray(place.path("regularOpeningHours").path("weekdayDescriptions")));
  }

  private List<String> stringArray(JsonNode values) {
    List<String> result = new ArrayList<>();
    if (!values.isArray()) return result;
    for (JsonNode value : values) result.add(value.asText(""));
    return result.stream().filter(StringUtils::hasText).toList();
  }

  private List<Map<String, Object>> reviewList(JsonNode reviews) {
    List<Map<String, Object>> values = new ArrayList<>();
    if (!reviews.isArray()) return values;
    for (JsonNode review : reviews) {
      values.add(mapOf(
          "author", text(review.path("authorAttribution").path("displayName"), ""),
          "rating", review.path("rating").asDouble(0),
          "text", text(review.path("text").path("text"), ""),
          "publishTime", review.path("publishTime").asText(""),
          "googleMapsUri", review.path("googleMapsUri").asText("")));
    }
    return values;
  }

  private static String text(JsonNode node, String fallback) {
    String value = node.asText("");
    return StringUtils.hasText(value) ? value : fallback;
  }
}
