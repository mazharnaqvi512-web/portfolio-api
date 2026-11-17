package com.mazhar.portfolio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mirrors the Angular side's ContactFormValue (src/app/core/models/contact-form.model.ts).
 * `honeypot` is not a real form field — it's a hidden input the Angular form renders
 * off-screen; a genuine visitor never fills it in, so any non-blank value here is a strong
 * bot signal (spec section 47). Frontend validation alone is not trusted — every constraint
 * here is re-checked server-side, and the size limits match the ContactMessage column lengths
 * so bad input gets a clean 400 rather than a database error.
 */
public record ContactRequest(
    @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must be at most 100 characters") String name,
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 320, message = "Email must be at most 320 characters")
    String email,
    @Size(max = 200, message = "Company must be at most 200 characters") String company,
    @NotBlank(message = "Subject is required") @Size(max = 200, message = "Subject must be at most 200 characters") String subject,
    @NotBlank(message = "Message is required")
    @Size(min = 20, max = 2000, message = "Message must be between 20 and 2000 characters")
    String message,
    @Size(max = 100, message = "Project type must be at most 100 characters") String projectType,
    @Size(max = 100, message = "Budget must be at most 100 characters") String budget,
    @Size(max = 100, message = "Timeline must be at most 100 characters") String timeline,
    String honeypot
) {}
