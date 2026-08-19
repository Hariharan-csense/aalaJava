package com.aalago.aalago.backend.service;

import static com.aalago.aalago.backend.util.ApiUtil.mapOf;

import com.aalago.aalago.backend.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {
  private final Path uploadDir;
  private final String publicUrl;

  public UploadService(
      @Value("${aalago.upload.dir:uploads}") String uploadDir,
      @Value("${aalago.public-url:}") String publicUrl) {
    this.uploadDir = Path.of(uploadDir);
    this.publicUrl = publicUrl == null ? "" : publicUrl.replaceAll("/+$", "");
  }

  public Map<String, Object> save(HttpServletRequest request, MultipartFile file) {
    if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Image file is required");
    if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
    }
    if (file.getSize() > 5L * 1024 * 1024) throw new ApiException(HttpStatus.BAD_REQUEST, "Image file is too large");
    String original = Optional.ofNullable(file.getOriginalFilename()).orElse("image");
    String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
    String base = original.replaceAll("\\.[^.]+$", "").toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    if (base.length() > 40) base = base.substring(0, 40);
    String filename = System.currentTimeMillis() + "-" + (StringUtils.hasText(base) ? base : "image") + ext;
    try {
      Files.createDirectories(uploadDir);
      file.transferTo(uploadDir.resolve(filename));
    } catch (IOException ex) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
    String baseUrl = StringUtils.hasText(publicUrl)
        ? publicUrl
        : request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    return mapOf("filename", filename, "url", baseUrl + "/uploads/" + filename);
  }
}
