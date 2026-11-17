package com.mazhar.portfolio.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mazhar.portfolio.dto.ApiResponse;
import com.mazhar.portfolio.dto.ContactRequest;
import com.mazhar.portfolio.service.ContactService;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Always returns success, even when the honeypot silently discards the
     * submission — revealing spam detection to the caller would just teach
     * bots to avoid it. Genuine validation failures (missing/invalid real
     * fields) still return 400 via GlobalExceptionHandler, since that
     * feedback is what a real visitor needs to fix their submission.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(@Valid @RequestBody ContactRequest request) {
        contactService.submit(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
