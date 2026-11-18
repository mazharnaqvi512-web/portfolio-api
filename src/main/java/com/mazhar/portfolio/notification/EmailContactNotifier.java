package com.mazhar.portfolio.notification;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import com.mazhar.portfolio.entity.ContactMessage;

/**
 * Emails the owner each new contact-form message, from a background thread so the visitor does
 * not wait on the mail server.
 *
 * <p>Everything the visitor typed is untrusted and ends up in an email header (subject) or in the
 * body. Headers are the dangerous part: a name containing a line break could otherwise inject
 * extra headers such as {@code Bcc:}. So control characters are stripped from anything used in the
 * subject, the visitor's address is only used as Reply-To if it parses strictly, and the body is
 * plain text.
 *
 * <p>Failures are logged by message id and exception type only (never the visitor's data) and
 * never propagate: the message is already safely stored.
 */
public class EmailContactNotifier implements ContactNotifier, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(EmailContactNotifier.class);

    private static final int MAX_SUBJECT_PART = 80;
    private static final long DRAIN_SECONDS_ON_CLOSE = 15;

    private final JavaMailSender mailSender;
    private final String to;
    private final String from;
    private final Executor executor;
    /** Set only when this notifier created its own thread (see withBackgroundThread); null when the caller supplied the executor. */
    private final ExecutorService ownedExecutor;

    public EmailContactNotifier(JavaMailSender mailSender, String to, String from, Executor executor) {
        this(mailSender, to, from, executor, null);
    }

    private EmailContactNotifier(JavaMailSender mailSender, String to, String from, Executor executor, ExecutorService ownedExecutor) {
        this.mailSender = mailSender;
        this.to = to;
        this.from = from;
        this.executor = executor;
        this.ownedExecutor = ownedExecutor;
    }

    /**
     * Sends from one background thread owned by the notifier. Contact mail is low volume (the endpoint is rate
     * limited) and order does not matter. The thread is deliberately not a Spring bean: any Executor bean would
     * make Spring Boot drop its own default task executor.
     */
    public static EmailContactNotifier withBackgroundThread(JavaMailSender mailSender, String to, String from) {
        ExecutorService thread = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable, "contact-mail");
            t.setDaemon(true);
            return t;
        });
        return new EmailContactNotifier(mailSender, to, from, thread, thread);
    }

    @Override
    public void newMessage(ContactMessage message) {
        executor.execute(() -> send(message));
    }

    /**
     * Called by Spring when the application shuts down. Lets notifications that are still queued finish (up to a
     * limit), so a redeploy does not silently drop a visitor's message email.
     */
    @Override
    public void close() throws InterruptedException {
        if (ownedExecutor == null) {
            return;
        }
        ownedExecutor.shutdown();
        if (!ownedExecutor.awaitTermination(DRAIN_SECONDS_ON_CLOSE, TimeUnit.SECONDS)) {
            log.warn("Contact notifications were still being sent at shutdown and were abandoned");
            ownedExecutor.shutdownNow();
        }
    }

    private void send(ContactMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("[Portfolio] " + headerSafe(message.getName()) + ": " + headerSafe(message.getSubject()));
            if (isStrictlyValidAddress(message.getEmail())) {
                helper.setReplyTo(message.getEmail());
            }
            helper.setText(body(message), false);
            mailSender.send(mime);
            log.info("Contact notification sent for message id {}", message.getId());
        } catch (Exception e) {
            // Exception type only: a provider's message can echo addresses or credentials' hints.
            log.warn("Contact notification could not be sent for message id {}: {}", message.getId(), e.getClass().getSimpleName());
        }
    }

    /** Control characters (line breaks above all) become spaces, then the text is shortened. */
    static String headerSafe(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]+", " ").strip();
        return cleaned.length() <= MAX_SUBJECT_PART ? cleaned : cleaned.substring(0, MAX_SUBJECT_PART - 1) + "…";
    }

    private static boolean isStrictlyValidAddress(String address) {
        if (address == null || address.chars().anyMatch(Character::isISOControl)) {
            return false;
        }
        try {
            new InternetAddress(address, true).validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String body(ContactMessage message) {
        StringBuilder text = new StringBuilder("New message from your portfolio contact form\n\n");
        text.append("Name: ").append(message.getName()).append('\n');
        text.append("Email: ").append(message.getEmail()).append('\n');
        appendIfPresent(text, "Company", message.getCompany());
        appendIfPresent(text, "Project type", message.getProjectType());
        appendIfPresent(text, "Budget", message.getBudget());
        appendIfPresent(text, "Timeline", message.getTimeline());
        text.append("Subject: ").append(message.getSubject()).append("\n\n");
        text.append("Message:\n").append(message.getMessage()).append("\n\n");
        text.append("---\nStored as message ").append(message.getId()).append(" at ").append(message.getCreatedAt())
            .append(". Reply to this email to answer the visitor directly.\n");
        return text.toString();
    }

    private static void appendIfPresent(StringBuilder text, String label, String value) {
        if (value != null && !value.isBlank()) {
            text.append(label).append(": ").append(value).append('\n');
        }
    }
}
