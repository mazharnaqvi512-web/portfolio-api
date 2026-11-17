package com.mazhar.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A stored contact-form submission. Holds private visitor data — never expose it through a public endpoint. */
@Entity
@Table(name = "contact_messages")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(length = 200)
    private String company;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(length = 100)
    private String projectType;

    @Column(length = 100)
    private String budget;

    @Column(length = 100)
    private String timeline;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Required by JPA. */
    protected ContactMessage() {}

    public ContactMessage(
        String name, String email, String company, String subject,
        String message, String projectType, String budget, String timeline
    ) {
        this.name = name;
        this.email = email;
        this.company = company;
        this.subject = subject;
        this.message = message;
        this.projectType = projectType;
        this.budget = budget;
        this.timeline = timeline;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCompany() { return company; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public String getProjectType() { return projectType; }
    public String getBudget() { return budget; }
    public String getTimeline() { return timeline; }
    public Instant getCreatedAt() { return createdAt; }
}
