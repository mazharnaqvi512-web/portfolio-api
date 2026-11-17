package com.mazhar.portfolio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import com.mazhar.portfolio.entity.ContactMessage;

/** Round-trips an entity through a real (in-memory) database — catches mapping and column-constraint mistakes mocks can't. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ContactMessageRepositoryTest {

    @Autowired
    private ContactMessageRepository repository;

    @Test
    void persistsAMessageAndAssignsIdAndTimestamp() {
        ContactMessage saved = repository.saveAndFlush(new ContactMessage(
            "Jane Doe", "jane@example.com", null, "Project inquiry",
            "A message that is definitely long enough to pass validation.", null, null, null
        ));

        ContactMessage found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getEmail()).isEqualTo("jane@example.com");
        assertThat(found.getCompany()).isNull();
    }
}
