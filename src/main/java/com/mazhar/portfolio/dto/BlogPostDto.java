package com.mazhar.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Must stay field-for-field compatible with the Angular side's BlogPost
 * model (src/app/core/models/blog-post.model.ts); see ApiContractTest.
 * `updatedAt` and `coverImage` are the optional fields there, so they're
 * omitted when null. `status` is "draft", "published" or "archived" — the
 * API only ever serves "published" posts (see BlogService).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BlogPostDto(
    String id,
    String slug,
    String title,
    String excerpt,
    String category,
    List<String> tags,
    String publishedAt,
    String updatedAt,
    int readingTime,
    String author,
    String coverImage,
    boolean featured,
    String status,
    String content
) {}
