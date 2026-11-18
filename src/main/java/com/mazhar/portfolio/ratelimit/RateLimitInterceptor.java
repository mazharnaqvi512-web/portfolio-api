package com.mazhar.portfolio.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import com.mazhar.portfolio.dto.ApiResponse;

/**
 * Runs before argument validation, so malformed submissions count toward the
 * limit too — otherwise a bot could probe the endpoint unthrottled with
 * invalid bodies. Keyed on the connection's remote address (an IPv6 client by its
 * /64 prefix, see ClientAddressKey); behind a reverse
 * proxy, configure server.forward-headers-strategy so that address is the
 * real client rather than the proxy (and don't read X-Forwarded-For here
 * directly — a client can forge it).
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final SlidingWindowRateLimiter limiter;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(SlidingWindowRateLimiter limiter, ObjectMapper objectMapper) {
        this.limiter = limiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        SlidingWindowRateLimiter.Decision decision = limiter.tryAcquire(ClientAddressKey.of(request.getRemoteAddr()));
        if (decision.allowed()) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
            ApiResponse.error("Too many requests. Please try again later.", List.of())
        ));
        return false;
    }
}
