package com.aalago.aalago.backend.config;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import com.aalago.aalago.backend.repository.MembershipRepository;
import com.aalago.aalago.backend.service.MembershipService;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseInitializer implements CommandLineRunner {
  private final JdbcTemplate jdbc;
  private final MembershipService memberships;

  public DatabaseInitializer(JdbcTemplate jdbc, MembershipService memberships) {
    this.jdbc = jdbc;
    this.memberships = memberships;
  }

  @Override
  @Transactional
  public void run(String... args) {
    schema();
    if (jdbc.queryForObject("select count(*) from membership_packages", Integer.class) == 0) {
      for (Map<String, Object> plan : defaultMemberships())
        memberships.create(plan);
    }
  }

  private void schema() {
    jdbc.execute(
        "create table if not exists destinations(id varchar(120) primary key, name varchar(191) not null, state varchar(191) not null, image text not null, description text not null, created_at timestamp default current_timestamp, updated_at timestamp default current_timestamp)");
    jdbc.execute(
        "create table if not exists properties(id varchar(120) primary key, destination_id varchar(120) not null, name varchar(191) not null, location varchar(191) not null, type varchar(80) not null, price decimal(10,2) not null default 0, rating decimal(3,2) not null default 0, reviews int unsigned not null default 0, popular boolean not null default false, image text not null, description text not null, booking_url text, google_place_id varchar(191), google_map_link text, google_review_link text, google_latitude decimal(10,7), google_longitude decimal(10,7), google_reviews_json text, created_at timestamp default current_timestamp, updated_at timestamp default current_timestamp)");
    jdbc.execute(
        "create table if not exists property_images(id int auto_increment primary key, property_id varchar(120) not null, image text not null, sort_order int unsigned not null default 0, created_at timestamp default current_timestamp, updated_at timestamp default current_timestamp)");
    jdbc.execute(
        "create table if not exists property_amenities(id int auto_increment primary key, property_id varchar(120) not null, name varchar(120) not null, sort_order int unsigned not null default 0, created_at timestamp default current_timestamp, updated_at timestamp default current_timestamp)");
    jdbc.execute(
        "create table if not exists property_highlights(id int auto_increment primary key, property_id varchar(120) not null, text varchar(255) not null, sort_order int unsigned not null default 0, created_at timestamp default current_timestamp, updated_at timestamp default current_timestamp)");
    jdbc.execute(
        "create table if not exists partner_enquiries(id int auto_increment primary key, name varchar(191) not null, phone_number varchar(40) not null, email varchar(191) not null, city varchar(120) not null, hotel_name varchar(191) not null, location_within_city varchar(191) not null, location_pin_code varchar(20) not null, property_age varchar(40) not null, number_of_rooms varchar(40) not null, crm_payload text, created_at timestamp default current_timestamp, updated_at timestamp default current_timestamp)");
    jdbc.execute(
        "create table if not exists subscribers(id int auto_increment primary key, email varchar(191) not null unique, source varchar(120) not null default 'Newsletter Banner', created_at timestamp default current_timestamp, updated_at timestamp default current_timestamp)");
    jdbc.execute(
        "create table if not exists membership_packages(id int auto_increment primary key, name varchar(191) not null, price decimal(10,2) not null default 0, period varchar(80) not null default 'Year', booking_discount text, reward_wallet varchar(80) not null default '', complimentary_breakfast varchar(80) not null default '', early_check_in_late_check_out varchar(80) not null default '', priority_booking varchar(40) not null default 'No', member_only_deals varchar(40) not null default 'Yes', travel_welcome_kit varchar(120) not null default 'No', priority_customer_support varchar(120) not null default 'No', popular boolean not null default false, sort_order int unsigned not null default 0, created_at timestamp default current_timestamp, updated_at timestamp default current_timestamp)");
    jdbc.execute(
        "create table if not exists membership_package_features(id int auto_increment primary key, membership_package_id int unsigned not null, text text not null, sort_order int unsigned not null default 0)");
    jdbc.execute(
        "create table if not exists membership_orders(id int auto_increment primary key, membership_package_id int not null, package_name varchar(191) not null, amount decimal(10,2) not null default 0, currency varchar(8) not null default 'INR', name varchar(191) not null, mobile_number varchar(40) not null, email varchar(191), location varchar(191) not null, razorpay_order_id varchar(80) not null unique, razorpay_payment_id varchar(80), razorpay_signature varchar(255), receipt varchar(40) not null unique, status varchar(40) not null default 'created', payment_date timestamp null, expiry_date timestamp null, renewal_date timestamp null, created_at timestamp default current_timestamp, updated_at timestamp default current_timestamp)");
    ensurePropertyGoogleColumns();
  }

  private void ensurePropertyGoogleColumns() {
    for (String sql : List.of(
        "alter table properties add column google_place_id varchar(191)",
        "alter table properties add column google_map_link text",
        "alter table properties add column google_review_link text",
        "alter table properties add column google_latitude decimal(10,7)",
        "alter table properties add column google_longitude decimal(10,7)",
        "alter table properties add column google_reviews_json text",
        "alter table properties add column google_details_json text")) {
      try {
        jdbc.execute(sql);
      } catch (Exception ignored) {
        // Column already exists.
      }
    }
  }

  private List<Map<String, Object>> defaultMemberships() {
    return List.of(
        membership("AalaGO Explorer", 499, false, 1, "10% OFF (Up to Rs.500/year)", "Rs.250", "1 Stay", "", "No", "Yes",
            "No", "No"),
        membership("AalaGO Premium", 999, true, 2, "15% OFF (Up to Rs.2,000/year)", "Rs.600", "2 Stays", "1 Time",
            "Yes", "Yes", "Yes", "Yes"),
        membership("AalaGO Legend", 1499, false, 3, "20% OFF (Up to Rs.3,000/year)", "Rs.1,000", "3 Stays", "3 Times",
            "Yes", "Yes", "Premium Kit", "Premium Support"));
  }

  private Map<String, Object> membership(String name, int price, boolean popular, int sortOrder, String discount,
      String wallet, String breakfast, String checkIn, String booking, String deals, String kit, String support) {
    Map<String, Object> benefits = mapOf("bookingDiscount", discount, "rewardWallet", wallet, "complimentaryBreakfast",
        breakfast, "earlyCheckInLateCheckOut", checkIn, "priorityBooking", booking, "memberOnlyDeals", deals,
        "travelWelcomeKit", kit, "priorityCustomerSupport", support);
    return mapOf("name", name, "price", price, "period", "Year", "popular", popular, "sortOrder", sortOrder, "features",
        MembershipRepository.featuresFromBenefits(benefits), "benefits", benefits);
  }
}
