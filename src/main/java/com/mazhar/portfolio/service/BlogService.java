package com.mazhar.portfolio.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mazhar.portfolio.content.ContentLoader;
import com.mazhar.portfolio.dto.BlogPostDto;

/**
 * Serves content/blog-posts.json, generated from the Angular side's
 * blog-posts.data.ts (empty until the first article is written).
 *
 * Only "published" posts ever leave this class. This is a public API with no
 * auth, so drafts and archived posts must be filtered here on the server —
 * relying on the Angular app to hide them would still ship them to anyone
 * who calls the endpoint directly.
 */
@Service
public class BlogService {

    private static final String PUBLISHED = "published";

    private final List<BlogPostDto> posts;

    @Autowired
    public BlogService(ContentLoader content) {
        this(content.loadList("blog-posts.json", BlogPostDto.class));
    }

    /** Test seam: lets tests supply posts, since the real list is empty until the first article exists. */
    BlogService(List<BlogPostDto> posts) {
        this.posts = posts;
    }

    public List<BlogPostDto> getAll() {
        return posts.stream().filter(BlogService::isPublished).toList();
    }

    public Optional<BlogPostDto> getBySlug(String slug) {
        return posts.stream()
            .filter(BlogService::isPublished)
            .filter(post -> post.slug().equals(slug))
            .findFirst();
    }

    private static boolean isPublished(BlogPostDto post) {
        return PUBLISHED.equals(post.status());
    }
}
