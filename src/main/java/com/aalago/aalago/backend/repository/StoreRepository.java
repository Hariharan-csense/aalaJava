package com.aalago.aalago.backend.repository;

import static com.aalago.aalago.backend.util.ApiUtil.*;

import com.aalago.aalago.backend.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class StoreRepository {

  private static final List<String> PAGE_SLUGS = List.of("home", "about", "banners");

  private final ObjectMapper mapper;
  private final Path dataPath;

  public StoreRepository(
      ObjectMapper mapper,
      @Value("${aalago.store.path:data/store.json}") String dataPath) {

    this.mapper = mapper;
    this.dataPath = Path.of(dataPath);
  }

  public synchronized Map<String, Object> read() {

    try {

      if (!Files.exists(dataPath)) {

        Files.createDirectories(dataPath.getParent());

        ClassPathResource resource = new ClassPathResource("store.json");

        try (InputStream inputStream = resource.getInputStream()) {

          Files.copy(
              inputStream,
              dataPath,
              StandardCopyOption.REPLACE_EXISTING);

        }
      }

      return mapper.readValue(
          dataPath.toFile(),
          new TypeReference<LinkedHashMap<String, Object>>() {
          });

    } catch (IOException ex) {

      throw new ApiException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          ex.getMessage());

    }
  }

  public synchronized void write(Map<String, Object> data) {

    try {

      Files.createDirectories(dataPath.getParent());

      mapper.writerWithDefaultPrettyPrinter()
          .writeValue(dataPath.toFile(), data);

    } catch (IOException ex) {

      throw new ApiException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          ex.getMessage());

    }
  }

  public List<Map<String, Object>> blogPosts() {
    return list(read().get("blogPosts"));
  }

  public Map<String, Object> createBlogPost(Map<String, Object> payload) {

    Map<String, Object> data = read();

    List<Map<String, Object>> posts = new ArrayList<>(list(data.get("blogPosts")));

    String id = required(payload.get("id"), "Blog id");

    if (posts.stream().anyMatch(post -> id.equals(post.get("id")))) {
      throw new ApiException(HttpStatus.CONFLICT, "Blog id already exists");
    }

    Map<String, Object> post = mapOf(
        "id", id,
        "title", required(payload.get("title"), "Blog title"),
        "excerpt", required(payload.get("excerpt"), "Blog excerpt"),
        "author", required(payload.get("author"), "Blog author"),
        "readTime", required(payload.get("readTime"), "Blog read time"),
        "category", required(payload.get("category"), "Blog category"),
        "date", required(payload.get("date"), "Blog date"),
        "image", optional(payload.get("image"), ""));

    posts.add(post);

    data.put("blogPosts", posts);

    write(data);

    return post;
  }

  public Map<String, Object> updateBlogPost(
      String id,
      Map<String, Object> payload) {

    Map<String, Object> data = read();

    List<Map<String, Object>> posts = new ArrayList<>(list(data.get("blogPosts")));

    for (Map<String, Object> post : posts) {

      if (id.equals(post.get("id"))) {

        for (String key : List.of(
            "title",
            "excerpt",
            "author",
            "readTime",
            "category",
            "date",
            "image")) {

          if (payload.containsKey(key)) {
            post.put(key, payload.get(key));
          }

        }

        data.put("blogPosts", posts);

        write(data);

        return post;
      }
    }

    throw new ApiException(
        HttpStatus.NOT_FOUND,
        "Blog post not found");
  }

  public void deleteBlogPost(String id) {

    Map<String, Object> data = read();

    List<Map<String, Object>> posts = new ArrayList<>(list(data.get("blogPosts")));

    boolean removed = posts.removeIf(post -> id.equals(post.get("id")));

    if (!removed) {

      throw new ApiException(
          HttpStatus.NOT_FOUND,
          "Blog post not found");
    }

    data.put("blogPosts", posts);

    write(data);
  }

  public Map<String, Object> pageContent(String slug) {

    if (!PAGE_SLUGS.contains(slug)) {
      throw new ApiException(
          HttpStatus.NOT_FOUND,
          "Page content not found");
    }

    Map<String, Object> data = read();

    Map<String, Object> pageContent = map(data.get("pageContent"));

    return new LinkedHashMap<>(
        map(pageContent.get(slug)));
  }

  public Map<String, Object> updatePageContent(
      String slug,
      Map<String, Object> payload) {

    if (!PAGE_SLUGS.contains(slug)) {
      throw new ApiException(
          HttpStatus.NOT_FOUND,
          "Page content not found");
    }

    Map<String, Object> data = read();

    Map<String, Object> pageContent = new LinkedHashMap<>(map(data.get("pageContent")));

    Map<String, Object> merged = new LinkedHashMap<>(map(pageContent.get(slug)));

    merged.putAll(payload);

    pageContent.put(slug, merged);

    data.put("pageContent", pageContent);

    write(data);

    return merged;
  }

  public Map<String, Object> admin() {
    return map(read().get("admin"));
  }

}