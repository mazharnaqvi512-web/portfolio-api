package com.mazhar.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.mazhar.portfolio.dto.ContactRequest;
import com.mazhar.portfolio.entity.ContactMessage;
import com.mazhar.portfolio.notification.ContactNotifier;
import com.mazhar.portfolio.repository.ContactMessageRepository;

/**
 * Persists genuine submissions (spec section 46: Contact Service -> Database) and then tells the
 * owner by email when mail is configured. There is deliberately no public endpoint for reading
 * stored messages, since there is no auth yet.
 */
@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactMessageRepository repository;
    private final ContactNotifier notifier;

    public ContactService(ContactMessageRepository repository, ContactNotifier notifier) {
        this.repository = repository;
        this.notifier = notifier;
    }

    /** @return true if the submission was stored, false if silently discarded as spam. */
    public boolean submit(ContactRequest request) {
        if (StringUtils.hasText(request.honeypot())) {
            log.warn("Contact form honeypot field was filled in — treating as spam, discarding silently.");
            return false;
        }

        ContactMessage saved = repository.save(toEntity(request));
        // Id only — visitors' names and emails are personal data and don't belong in logs.
        log.info("Contact form submission stored — id: {}", saved.getId());
        notifyOwner(saved);
        return true;
    }

    /** The message is already stored, so a notification problem must never turn into an error for the visitor. */
    private void notifyOwner(ContactMessage saved) {
        try {
            notifier.newMessage(saved);
        } catch (RuntimeException e) {
            log.warn("Contact notification failed for message id {}: {}", saved.getId(), e.getClass().getSimpleName());
        }
    }

    private ContactMessage toEntity(ContactRequest request) {
        return new ContactMessage(
            request.name(), request.email(), request.company(), request.subject(),
            request.message(), request.projectType(), request.budget(), request.timeline()
        );
    }
}
