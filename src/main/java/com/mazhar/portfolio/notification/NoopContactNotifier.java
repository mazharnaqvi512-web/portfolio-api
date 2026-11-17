package com.mazhar.portfolio.notification;

import com.mazhar.portfolio.entity.ContactMessage;

/** Used when email is not configured: messages are still stored, just not announced. */
public class NoopContactNotifier implements ContactNotifier {

    @Override
    public void newMessage(ContactMessage message) {
        // Intentionally nothing.
    }
}
