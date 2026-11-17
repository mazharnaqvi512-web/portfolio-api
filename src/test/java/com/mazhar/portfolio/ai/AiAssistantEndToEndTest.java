package com.mazhar.portfolio.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** The whole application with no API key: the assistant must say it is off, not pretend to answer. */
@SpringBootTest
@AutoConfigureMockMvc
class AiAssistantEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiChatProvider provider;

    @Test
    void withoutAnApiKeyTheContextUsesTheUnconfiguredProvider() {
        assertThat(provider).isInstanceOf(UnconfiguredChatProvider.class);
    }

    @Test
    void withoutAnApiKeyTheEndpointAnswers503() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"messages\":[{\"role\":\"user\",\"content\":\"What does he do?\"}]}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", is("The AI assistant is not configured on this server.")));
    }

    @Test
    void anInvalidChatIsStillA400EvenWhenTheAssistantIsOff() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"messages\":[]}"))
            .andExpect(status().isBadRequest());
    }
}
