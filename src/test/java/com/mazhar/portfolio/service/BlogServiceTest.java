package com.mazhar.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.mazhar.portfolio.content.ContentLoader;
import com.mazhar.portfolio.dto.BlogPostDto;

class BlogServiceTest {

    private static BlogPostDto post(String slug, String status) {
        return new BlogPostDto(
            "id-" + slug, slug, "Title " + slug, "Excerpt", "angular", List.of("angular"),
            "2026-01-01", null, 5, "Syed Mazhar Hussain", null, false, status, "Content"
        );
    }

    private final BlogService blogService = new BlogService(List.of(
        post("published-post", "published"),
        post("draft-post", "draft"),
        post("archived-post", "archived")
    ));

    @Test
    void listsOnlyPublishedPosts() {
        assertThat(blogService.getAll()).extracting(BlogPostDto::slug).containsExactly("published-post");
    }

    @Test
    void findsAPublishedPostBySlug() {
        assertThat(blogService.getBySlug("published-post")).isPresent();
    }

    @Test
    void neverRevealsADraftEvenWhenRequestedBySlug() {
        assertThat(blogService.getBySlug("draft-post")).isEmpty();
    }

    @Test
    void neverRevealsAnArchivedPostEvenWhenRequestedBySlug() {
        assertThat(blogService.getBySlug("archived-post")).isEmpty();
    }

    @Test
    void theShippedContentLoadsAndExposesOnlyPublishedPosts() {
        BlogService shipped = new BlogService(new ContentLoader());

        assertThat(shipped.getAll()).allSatisfy(post -> assertThat(post.status()).isEqualTo("published"));
    }
}
