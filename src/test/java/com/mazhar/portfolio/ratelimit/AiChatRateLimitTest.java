package com.mazhar.portfolio.ratelimit;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.mazhar.portfolio.ai.AiChatService;
import com.mazhar.portfolio.controller.AiController;

@WebMvcTest(value = AiController.class, properties = {
    "ai.rate-limit.max-requests=2",
    "ai.rate-limit.window=10m"
})
class AiChatRateLimitTest {

    private static final String BODY = "{\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiChatService chatService;

    private static RequestPostProcessor fromAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    @Test
    void rejectsTheRequestOverTheLimitWithA429AndRetryAfterBeforeAnyModelCall() throws Exception {
        when(chatService.chat(anyList())).thenReturn("ok");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/ai/chat").with(fromAddress("10.1.0.1"))
                    .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/ai/chat").with(fromAddress("10.1.0.1"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void limitsEachClientAddressIndependently() throws Exception {
        when(chatService.chat(anyList())).thenReturn("ok");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/ai/chat").with(fromAddress("10.1.0.2"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY));
        }

        mockMvc.perform(post("/api/ai/chat").with(fromAddress("10.1.0.2"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isTooManyRequests());
        mockMvc.perform(post("/api/ai/chat").with(fromAddress("10.1.0.3"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isOk());
    }

    @Test
    void anIpv6ClientCannotDodgeTheLimitByChangingAddressWithinItsOwn64() throws Exception {
        when(chatService.chat(anyList())).thenReturn("ok");

        mockMvc.perform(post("/api/ai/chat").with(fromAddress("2001:db8:aaaa:bbbb::1"))
            .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());
        mockMvc.perform(post("/api/ai/chat").with(fromAddress("2001:db8:aaaa:bbbb::2"))
            .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());

        // Same /64, third request: over the limit of 2, whatever the low bits say.
        mockMvc.perform(post("/api/ai/chat").with(fromAddress("2001:db8:aaaa:bbbb:1234:5678:9abc:def0"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isTooManyRequests());

        // A different /64 is a different client.
        mockMvc.perform(post("/api/ai/chat").with(fromAddress("2001:db8:aaaa:cccc::1"))
            .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());
    }

    @Test
    void countsInvalidRequestsToo() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/ai/chat").with(fromAddress("10.1.0.4"))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/ai/chat").with(fromAddress("10.1.0.4"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isTooManyRequests());
    }
}
