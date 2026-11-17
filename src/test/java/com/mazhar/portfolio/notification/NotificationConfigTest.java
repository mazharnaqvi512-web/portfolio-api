package com.mazhar.portfolio.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import com.mazhar.portfolio.entity.ContactMessage;

class NotificationConfigTest {

    @RegisterExtension
    GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort());

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final NotificationConfig config = new NotificationConfig();

    @AfterEach
    void shutDown() {
        executor.shutdownNow();
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return provider;
    }

    private JavaMailSenderImpl senderOnGreenMail() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(greenMail.getSmtp().getPort());
        return sender;
    }

    private ContactNotifier notifier(JavaMailSender sender, String to, String from, String host) {
        return config.contactNotifier(provider(sender), executor, to, from, host);
    }

    @Test
    void isOffWithoutARecipient() {
        assertThat(notifier(senderOnGreenMail(), "", "", "localhost")).isInstanceOf(NoopContactNotifier.class);
        assertThat(notifier(senderOnGreenMail(), "   ", "", "localhost")).isInstanceOf(NoopContactNotifier.class);
    }

    @Test
    void isOffWithoutAMailServer() {
        assertThat(notifier(senderOnGreenMail(), "owner@portfolio.test", "", "")).isInstanceOf(NoopContactNotifier.class);
    }

    @Test
    void isOffWhenNoMailSenderIsAvailable() {
        assertThat(notifier(null, "owner@portfolio.test", "", "localhost")).isInstanceOf(NoopContactNotifier.class);
    }

    @Test
    void isOnWithARecipientAndAMailServer() {
        assertThat(notifier(senderOnGreenMail(), "owner@portfolio.test", "site@portfolio.test", "localhost"))
            .isInstanceOf(EmailContactNotifier.class);
    }

    @Test
    void sendsFromTheRecipientsOwnAddressWhenNoSenderIsConfigured() throws Exception {
        ContactNotifier notifier = notifier(senderOnGreenMail(), "owner@portfolio.test", "", "localhost");

        notifier.newMessage(new ContactMessage(
            "Jane Doe", "jane@example.com", null, "Hello",
            "A message that is definitely long enough to pass validation.", null, null, null));

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        assertThat(greenMail.getReceivedMessages()[0].getFrom()[0].toString()).isEqualTo("owner@portfolio.test");
    }
}
