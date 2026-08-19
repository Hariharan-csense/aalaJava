package com.aalago.aalago.backend.service;

import com.aalago.aalago.backend.repository.ContentRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ContentService {
  private final ContentRepository repository;
  private final GooglePlacesService googlePlaces;

  public ContentService(ContentRepository repository, GooglePlacesService googlePlaces) {
    this.repository = repository;
    this.googlePlaces = googlePlaces;
  }

  public List<Map<String, Object>> destinations() {
    return repository.destinations();
  }

  public Optional<Map<String, Object>> property(String id) {
    return repository.property(id);
  }

  public List<Map<String, Object>> properties(String destinationId) {
    return repository.properties(destinationId);
  }

  public Map<String, Object> createDestination(Map<String, Object> payload) {
    return repository.createDestination(payload);
  }

  public Map<String, Object> updateDestination(String id, Map<String, Object> payload) {
    return repository.updateDestination(id, payload);
  }

  public void deleteDestination(String id) {
    repository.deleteDestination(id);
  }

  public Map<String, Object> createProperty(Map<String, Object> payload) {
    return repository.createProperty(googlePlaces.enrichProperty(payload));
  }

  public Map<String, Object> updateProperty(String id, Map<String, Object> payload) {
    return repository.updateProperty(id, googlePlaces.enrichProperty(payload));
  }

  public void deleteProperty(String id) {
    repository.deleteProperty(id);
  }

  public Map<String, Object> createPartnerEnquiry(Map<String, Object> payload) {
    return repository.createPartnerEnquiry(payload);
  }

  public List<Map<String, Object>> partnerEnquiries() {
    return repository.partnerEnquiries();
  }

  public Map<String, Object> createSubscriber(Map<String, Object> payload) {
    return repository.createSubscriber(payload);
  }

  public List<Map<String, Object>> subscribers() {
    return repository.subscribers();
  }
}
