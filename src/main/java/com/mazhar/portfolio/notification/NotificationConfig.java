package com.mazhar.portfolio.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Turns on emailing only when both a recipient (contact.notify.to) and a mail server
 * (spring.mail.host) are configured. Otherwise messages are just stored. Credentials come from
 * configuration (normally environment variables) and are never logged.
 */
@Configuration
public class NotificationConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationConfig.class);

    @Bean
    ContactNotifier contactNotifier(
        ObjectProvider<JavaMailSender> mailSender,
        @Value("${contact.notify.to:}") String to,
        @Value("${contact.notify.from:}") String from,
        @Value("${spring.mail.host:}") String host
    ) {
        if (isBlank(to) || isBlank(host)) {
            log.info("Contact email notifications disabled: set contact.notify.to and spring.mail.host to enable them");
            return new NoopContactNotifier();
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Contact email notifications disabled: no mail sender is available");
            return new NoopContactNotifier();
        }
        // Mail providers such as Gmail only accept the authenticated account as the sender; with no
        // explicit sender, fall back to the recipient (the owner's own address), which also works there.
        String effectiveFrom = isBlank(from) ? to : from;
        log.info("Contact email notifications enabled (mail host {})", host);
        // The notifier is AutoCloseable, so Spring closes it at shutdown and it finishes any queued mail first.
        return EmailContactNotifier.withBackgroundThread(sender, to, effectiveFrom);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
