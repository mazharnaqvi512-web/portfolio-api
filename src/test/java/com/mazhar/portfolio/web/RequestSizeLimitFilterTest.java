package com.mazhar.portfolio.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.mazhar.portfolio.exception.RequestTooLargeException;

class RequestSizeLimitFilterTest {

    private static final long LIMIT = 100;

    private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(LIMIT, new ObjectMapper());

    /** A request that either declares its length or, like a chunked upload, does not. */
    private static MockHttpServletRequest request(String method, String path, byte[] body, boolean declareLength) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path) {
            @Override
            public long getContentLengthLong() {
                return declareLength ? super.getContentLengthLong() : -1;
            }
        };
        request.setContent(body);
        return request;
    }

    private static byte[] bytes(int n) {
        return "x".repeat(n).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void refusesADeclaredLengthOverTheLimitWithoutReadingTheBodyOrCallingTheChain() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request("POST", "/api/contact", bytes(101), true), response, (req, res) -> chainCalled.set(true));

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chainCalled).isFalse();
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"success\":false", "The request is too large.");
    }

    @Test
    void cutsOffABodyWithNoDeclaredLengthOnceItPassesTheLimit() {
        MockHttpServletRequest request = request("POST", "/api/ai/chat", bytes(101), false);

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> req.getInputStream().readAllBytes()))
            .isInstanceOf(RequestTooLargeException.class);
    }

    @Test
    void aBodyExactlyAtTheLimitIsAcceptedWhetherOrNotItsLengthIsDeclared() throws Exception {
        for (boolean declared : new boolean[] {true, false}) {
            int[] seen = {0};
            filter.doFilter(request("POST", "/api/contact", bytes((int) LIMIT), declared), new MockHttpServletResponse(),
                (req, res) -> seen[0] = req.getInputStream().readAllBytes().length);

            assertThat(seen[0]).as("declared=%s", declared).isEqualTo((int) LIMIT);
        }
    }

    @Test
    void theByteByByteReadPathIsCountedToo() {
        RequestSizeLimitFilter.LimitedInputStream stream = new RequestSizeLimitFilter.LimitedInputStream(
            new DelegatingServletInputStream(new ByteArrayInputStream(bytes(5))), 3);

        assertThatThrownBy(() -> {
            while (stream.read() >= 0) {
                // keep reading
            }
        }).isInstanceOf(RequestTooLargeException.class);
    }

    @Test
    void otherPathsAndMethodsAreNotLimited() throws Exception {
        for (MockHttpServletRequest other : new MockHttpServletRequest[] {
            request("POST", "/api/projects", bytes(500), true),
            request("GET", "/api/contact", bytes(500), true),
            request("POST", "/actuator/health", bytes(500), true)}) {
            AtomicBoolean chainCalled = new AtomicBoolean();

            filter.doFilter(other, new MockHttpServletResponse(), (req, res) -> chainCalled.set(true));

            assertThat(chainCalled).as(other.getMethod() + " " + other.getRequestURI()).isTrue();
        }
    }
}
