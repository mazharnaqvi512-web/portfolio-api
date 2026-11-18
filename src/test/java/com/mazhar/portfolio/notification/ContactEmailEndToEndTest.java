package com.mazhar.portfolio.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.mazhar.portfolio.repository.ContactMessageRepository;

/** The whole path: the real endpoint, the real database, the real mail sender, an in-process SMTP server. */
@SpringBootTest
@AutoConfigureMockMvc
class ContactEmailEndToEndTest {

    @RegisterExtension
    // Class-level lifecycle: the SMTP port must exist before Spring builds the context that points at it.
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort()).withPerMethodLifecycle(false);

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
        registry.add("contact.notify.to", () -> "owner@portfolio.test");
        registry.add("contact.notify.from", () -> "site@portfolio.test");
        registry.add("contact.rate-limit.max-requests", () -> "1000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactMessageRepository repository;

    @Autowired
    private ContactNotifier notifier;

    @Autowired
    private ApplicationContext context;

    @BeforeEach
    void emptyInbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    private static String submission(String name, String honeypot) {
        return """
            {"name":"%s","email":"jane@example.com","subject":"Project inquiry",
             "message":"I would like to talk about an Angular project for next quarter.",
             "honeypot":%s}""".formatted(name, honeypot == null ? "null" : "\"" + honeypot + "\"");
    }

    @Test
    void emailingIsSwitchedOnByTheConfiguration() {
        assertThat(notifier).isInstanceOf(EmailContactNotifier.class);
    }

    @Test
    void theMailThreadIsNotAnExecutorBeanThatWouldDisplaceBootsDefaultTaskExecutor() {
        assertThat(context.containsBean("contactMailExecutor")).isFalse();
        assertThat(context.containsBean("applicationTaskExecutor")).isTrue();
    }

    @Test
    void aSubmissionIsStoredAndEmailedToTheOwner() throws Exception {
        long before = repository.count();

        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(submission("Jane Doe", null)))
            .andExpect(status().isOk());

        assertThat(repository.count()).isEqualTo(before + 1);
        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage mail = greenMail.getReceivedMessages()[0];
        assertThat(mail.getSubject()).isEqualTo("[Portfolio] Jane Doe: Project inquiry");
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("owner@portfolio.test");
        assertThat(mail.getReplyTo()[0].toString()).isEqualTo("jane@example.com");
        assertThat(GreenMailUtil.getBody(mail)).contains("I would like to talk about an Angular project");
    }

    @Test
    void aSpamSubmissionIsNeitherStoredNorEmailed() throws Exception {
        long before = repository.count();

        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(submission("Bot", "http://spam.test")))
            .andExpect(status().isOk());

        assertThat(repository.count()).isEqualTo(before);
        assertThat(greenMail.waitForIncomingEmail(1000, 1)).isFalse();
    }
}
