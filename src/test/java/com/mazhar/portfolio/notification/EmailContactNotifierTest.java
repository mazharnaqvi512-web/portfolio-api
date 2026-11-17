package com.mazhar.portfolio.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import com.mazhar.portfolio.entity.ContactMessage;

/** Sends real mail to an in-process SMTP server and reads it back, so the tests see exactly what an inbox would. */
class EmailContactNotifierTest {

    @RegisterExtension
    GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort());

    private EmailContactNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = notifierOnPort(greenMail.getSmtp().getPort());
    }

    private static EmailContactNotifier notifierOnPort(int port) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(port);
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.connectiontimeout", "1000");
        properties.put("mail.smtp.timeout", "1000");
        // Same thread, so the test can read the inbox straight after the call.
        return new EmailContactNotifier(sender, "owner@portfolio.test", "site@portfolio.test", Runnable::run);
    }

    private static ContactMessage message(String name, String email, String subject) {
        ContactMessage message = new ContactMessage(
            name, email, "Acme Inc", subject,
            "I would like to talk about an Angular project for next quarter.",
            "Web App", "$5k-10k", "1 month");
        ReflectionTestUtils.setField(message, "id", 42L);
        return message;
    }

    private MimeMessage onlyReceivedMessage() {
        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        return received[0];
    }

    @Test
    void deliversOneEmailToTheOwnerFromTheSiteAddress() throws Exception {
        notifier.newMessage(message("Jane Doe", "jane@example.com", "Project inquiry"));

        MimeMessage mail = onlyReceivedMessage();
        assertThat(mail.getAllRecipients()).hasSize(1);
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("owner@portfolio.test");
        assertThat(mail.getFrom()[0].toString()).isEqualTo("site@portfolio.test");
        assertThat(mail.getSubject()).isEqualTo("[Portfolio] Jane Doe: Project inquiry");
    }

    @Test
    void repliesGoStraightToTheVisitor() throws Exception {
        notifier.newMessage(message("Jane Doe", "jane@example.com", "Project inquiry"));

        assertThat(onlyReceivedMessage().getReplyTo()[0].toString()).isEqualTo("jane@example.com");
    }

    @Test
    void theBodyIsPlainTextWithEveryFieldTheVisitorFilledIn() throws Exception {
        notifier.newMessage(message("Jane Doe", "jane@example.com", "Project inquiry"));

        MimeMessage mail = onlyReceivedMessage();
        String body = GreenMailUtil.getBody(mail);
        assertThat(mail.getContentType()).startsWith("text/plain");
        assertThat(body).contains(
            "Name: Jane Doe", "Email: jane@example.com", "Company: Acme Inc", "Project type: Web App",
            "Budget: $5k-10k", "Timeline: 1 month", "Subject: Project inquiry",
            "I would like to talk about an Angular project for next quarter.", "message 42");
    }

    @Test
    void leavesOutOptionalFieldsTheVisitorSkipped() throws Exception {
        ContactMessage minimal = new ContactMessage(
            "Jane Doe", "jane@example.com", null, "Hello",
            "A message that is definitely long enough to pass validation.", null, "  ", null);

        notifier.newMessage(minimal);

        String body = GreenMailUtil.getBody(onlyReceivedMessage());
        assertThat(body).doesNotContain("Company:", "Project type:", "Budget:", "Timeline:");
    }

    @Test
    void aLineBreakInTheNameOrSubjectCannotInjectHeadersSuchAsBcc() throws Exception {
        notifier.newMessage(message(
            "Evil\r\nBcc: attacker@evil.test", "jane@example.com", "Hi\r\nTo: attacker@evil.test\r\n\r\ninjected body"));

        MimeMessage mail = onlyReceivedMessage();
        assertThat(mail.getSubject()).doesNotContain("\r", "\n");
        assertThat(mail.getHeader("Bcc")).isNull();
        assertThat(mail.getAllRecipients()).hasSize(1);
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("owner@portfolio.test");
    }

    @Test
    void anUnparseableVisitorAddressIsNotUsedAsReplyToButTheMailStillArrives() throws Exception {
        notifier.newMessage(message("Jane Doe", "jane@example.com\r\nBcc: attacker@evil.test", "Hello"));

        MimeMessage mail = onlyReceivedMessage();
        assertThat(mail.getHeader("Reply-To")).isNull();
        assertThat(mail.getHeader("Bcc")).isNull();
    }

    @Test
    void aVeryLongSubjectIsShortened() throws Exception {
        notifier.newMessage(message("Jane Doe", "jane@example.com", "S".repeat(200)));

        assertThat(onlyReceivedMessage().getSubject().length()).isLessThan(120);
    }

    @Test
    void anUnreachableMailServerNeverThrowsToTheCaller() {
        EmailContactNotifier deadServer = notifierOnPort(9);

        deadServer.newMessage(message("Jane Doe", "jane@example.com", "Hello"));

        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    @Test
    void headerSafeTurnsControlCharactersIntoSpacesAndTrims() {
        assertThat(EmailContactNotifier.headerSafe("  a\r\nb\tc  ")).isEqualTo("a b c");
        assertThat(EmailContactNotifier.headerSafe(null)).isEmpty();
    }
}
