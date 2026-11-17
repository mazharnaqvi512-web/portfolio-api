package com.mazhar.portfolio.notification;

import com.mazhar.portfolio.entity.ContactMessage;

/**
 * Tells the site owner that a visitor has written. Called after the message is already stored,
 * so an implementation must never make the visitor's request fail: whatever goes wrong here
 * is the owner's problem to see in the log, not the visitor's.
 */
public interface ContactNotifier {

    void newMessage(ContactMessage message);
}
