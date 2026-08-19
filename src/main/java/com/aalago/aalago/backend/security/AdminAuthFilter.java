package com.aalago.aalago.backend.security;

import com.aalago.aalago.backend.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminAuthFilter extends OncePerRequestFilter {
  private final AuthService auth;

  public AdminAuthFilter(AuthService auth) {
    this.auth = auth;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    boolean adminPath = path.startsWith("/api/admin") && !path.equals("/api/admin/login");
    if (!adminPath || HttpMethod.OPTIONS.matches(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ") || !auth.valid(header.substring(7))) {
      response.setStatus(401);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"message\":\"Invalid or expired token\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
