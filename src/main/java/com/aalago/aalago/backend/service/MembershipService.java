package com.aalago.aalago.backend.service;

import com.aalago.aalago.backend.repository.MembershipRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MembershipService {
  private final MembershipRepository repository;

  public MembershipService(MembershipRepository repository) {
    this.repository = repository;
  }

  public List<Map<String, Object>> packages() {
    return repository.packages();
  }

  public Map<String, Object> create(Map<String, Object> payload) {
    return repository.create(payload);
  }

  public Map<String, Object> update(Integer id, Map<String, Object> payload) {
    return repository.update(id, payload);
  }

  public void delete(Integer id) {
    repository.delete(id);
  }
}
