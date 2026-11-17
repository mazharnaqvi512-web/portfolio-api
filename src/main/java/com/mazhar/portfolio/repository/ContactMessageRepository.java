package com.mazhar.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mazhar.portfolio.entity.ContactMessage;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}
