package com.aalago.aalago.backend.service;

import com.aalago.aalago.backend.repository.StoreRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StoreService {
  private final StoreRepository repository;

  public StoreService(StoreRepository repository) {
    this.repository = repository;
  }

  public Map<String, Object> read() {
    return repository.read();
  }

  public List<Map<String, Object>> blogPosts() {
    return repository.blogPosts();
  }

  public Map<String, Object> createBlogPost(Map<String, Object> payload) {
    return repository.createBlogPost(payload);
  }

  public Map<String, Object> updateBlogPost(String id, Map<String, Object> payload) {
    return repository.updateBlogPost(id, payload);
  }

  public void deleteBlogPost(String id) {
    repository.deleteBlogPost(id);
  }

  public Map<String, Object> pageContent(String slug) {
    return repository.pageContent(slug);
  }

  public Map<String, Object> updatePageContent(String slug, Map<String, Object> payload) {
    return repository.updatePageContent(slug, payload);
  }

  public Map<String, Object> admin() {
    return repository.admin();
  }
}
