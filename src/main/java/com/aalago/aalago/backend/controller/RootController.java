package com.aalago.aalago.backend.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
  @GetMapping("/")
  Map<String, String> root() {
    return Map.of("message", "aalaGO API is running");
  }

  
}

