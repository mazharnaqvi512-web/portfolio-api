package com.mazhar.portfolio.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Per-address limits on the endpoints that accept input: POST /api/contact
 * (contact.rate-limit.*) and POST /api/ai/chat (ai.rate-limit.*, kept separate and tighter
 * because every chat call costs money). Each path has its own limiter, so a visitor who
 * has used up one is not locked out of the other.
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;
    private final int contactMaxRequests;
    private final Duration contactWindow;
    private final int chatMaxRequests;
    private final Duration chatWindow;

    public RateLimitConfig(
        ObjectMapper objectMapper,
        @Value("${contact.rate-limit.max-requests:5}") int contactMaxRequests,
        @Value("${contact.rate-limit.window:10m}") Duration contactWindow,
        @Value("${ai.rate-limit.max-requests:10}") int chatMaxRequests,
        @Value("${ai.rate-limit.window:10m}") Duration chatWindow
    ) {
        this.objectMapper = objectMapper;
        this.contactMaxRequests = contactMaxRequests;
        this.contactWindow = contactWindow;
        this.chatMaxRequests = chatMaxRequests;
        this.chatWindow = chatWindow;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptorFor(contactMaxRequests, contactWindow)).addPathPatterns("/api/contact");
        registry.addInterceptor(interceptorFor(chatMaxRequests, chatWindow)).addPathPatterns("/api/ai/chat");
    }

    private RateLimitInterceptor interceptorFor(int maxRequests, Duration window) {
        return new RateLimitInterceptor(new SlidingWindowRateLimiter(maxRequests, window, Clock.systemUTC()), objectMapper);
    }
}
