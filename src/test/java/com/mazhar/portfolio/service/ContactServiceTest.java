package com.mazhar.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.mazhar.portfolio.dto.ContactRequest;
import com.mazhar.portfolio.entity.ContactMessage;
import com.mazhar.portfolio.notification.ContactNotifier;
import com.mazhar.portfolio.repository.ContactMessageRepository;

class ContactServiceTest {

    private final ContactMessageRepository repository = mock(ContactMessageRepository.class);
    private final ContactNotifier notifier = mock(ContactNotifier.class);
    private final ContactService contactService = new ContactService(repository, notifier);

    private static ContactRequest genuineRequest() {
        return new ContactRequest(
            "Jane Doe", "jane@example.com", "Acme Inc", "Project inquiry",
            "A message that is definitely long enough to pass validation.",
            "Web App", "$5k-10k", "1 month", null
        );
    }

    @Test
    void storesAGenuineSubmission() {
        when(repository.save(any(ContactMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(contactService.submit(genuineRequest())).isTrue();

        ArgumentCaptor<ContactMessage> saved = ArgumentCaptor.forClass(ContactMessage.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Jane Doe");
        assertThat(saved.getValue().getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getValue().getSubject()).isEqualTo("Project inquiry");
        assertThat(saved.getValue().getCompany()).isEqualTo("Acme Inc");
    }

    @Test
    void tellsTheOwnerAboutTheStoredMessage() {
        when(repository.save(any(ContactMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contactService.submit(genuineRequest());

        ArgumentCaptor<ContactMessage> announced = ArgumentCaptor.forClass(ContactMessage.class);
        verify(notifier).newMessage(announced.capture());
        assertThat(announced.getValue().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void stillSucceedsWhenTheNotifierBlowsUpBecauseTheMessageIsAlreadySaved() {
        when(repository.save(any(ContactMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("mail server on fire")).when(notifier).newMessage(any(ContactMessage.class));

        assertThat(contactService.submit(genuineRequest())).isTrue();
        verify(repository).save(any(ContactMessage.class));
    }

    @Test
    void neverStoresOrAnnouncesASubmissionWithTheHoneypotFilledIn() {
        ContactRequest request = new ContactRequest(
            "Bot", "bot@example.com", null, "Spam",
            "A message that is definitely long enough to pass validation.",
            null, null, null, "http://spam.example.com"
        );

        assertThat(contactService.submit(request)).isFalse();
        verify(repository, never()).save(any(ContactMessage.class));
        verify(notifier, never()).newMessage(any(ContactMessage.class));
    }
}
