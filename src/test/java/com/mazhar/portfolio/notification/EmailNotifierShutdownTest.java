package com.mazhar.portfolio.notification;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import com.mazhar.portfolio.entity.ContactMessage;

/** A redeploy must not silently drop contact emails that are still waiting to be sent. */
class EmailNotifierShutdownTest {

    private final AtomicInteger sent = new AtomicInteger();

    /** A mail sender that takes 150 ms per message, so messages are still queued when shutdown starts. */
    private JavaMailSenderImpl slowSender() {
        return new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mimeMessage) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                sent.incrementAndGet();
            }
        };
    }

    private static ContactMessage message() {
        return new ContactMessage(
            "Jane Doe", "jane@example.com", null, "Hello",
            "A message that is definitely long enough to pass validation.", null, null, null);
    }

    @Test
    void closingTheNotifierLetsEveryQueuedEmailFinish() throws Exception {
        EmailContactNotifier notifier = EmailContactNotifier.withBackgroundThread(slowSender(), "owner@portfolio.test", "site@portfolio.test");

        for (int i = 0; i < 3; i++) {
            notifier.newMessage(message());
        }
        notifier.close();

        assertThat(sent.get()).isEqualTo(3);
    }

    @Test
    void springClosesTheNotifierWhenTheApplicationShutsDown() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("contactNotifier", ContactNotifier.class,
            () -> EmailContactNotifier.withBackgroundThread(slowSender(), "owner@portfolio.test", "site@portfolio.test"));
        context.refresh();
        ContactNotifier notifier = context.getBean(ContactNotifier.class);

        for (int i = 0; i < 3; i++) {
            notifier.newMessage(message());
        }
        context.close();

        assertThat(sent.get()).isEqualTo(3);
    }

    @Test
    void closeIsANoOpWhenTheCallerSuppliedTheExecutor() throws Exception {
        EmailContactNotifier notifier = new EmailContactNotifier(slowSender(), "owner@portfolio.test", "site@portfolio.test", Runnable::run);

        notifier.newMessage(message());
        notifier.close();
        notifier.newMessage(message());

        assertThat(sent.get()).isEqualTo(2);
    }
}
