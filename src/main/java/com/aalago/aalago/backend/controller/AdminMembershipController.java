package com.aalago.aalago.backend.controller;

import com.aalago.aalago.backend.dto.ApiResponse;
import com.aalago.aalago.backend.dto.MembershipPackageDto;
import com.aalago.aalago.backend.service.MembershipService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/memberships")
public class AdminMembershipController {
  private final MembershipService memberships;

  public AdminMembershipController(MembershipService memberships) {
    this.memberships = memberships;
  }

  @GetMapping
  ApiResponse<List<MembershipPackageDto>> list() {
    return new ApiResponse<>(memberships.packages().stream().map(MembershipPackageDto::from).toList());
  }

  @PostMapping
  ResponseEntity<ApiResponse<MembershipPackageDto>> create(@RequestBody Map<String, Object> payload) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(MembershipPackageDto.from(memberships.create(payload))));
  }

  @PutMapping("/{id}")
  ApiResponse<MembershipPackageDto> update(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
    return new ApiResponse<>(MembershipPackageDto.from(memberships.update(id, payload)));
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(@PathVariable Integer id) {
    memberships.delete(id);
    return ResponseEntity.noContent().build();
  }
}
