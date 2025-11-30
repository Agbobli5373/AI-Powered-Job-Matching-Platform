package com.isaac.job_matching.user.internal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Internal service for password encoding and verification.
 * 
 * <p>
 * This service wraps Spring Security's PasswordEncoder to provide
 * a clean interface for the User module. It uses BCrypt for hashing.
 */
@Service
public class PasswordEncodingService {

    private final PasswordEncoder passwordEncoder;

    public PasswordEncodingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Encodes a plaintext password using BCrypt.
     * 
     * @param rawPassword the plaintext password
     * @return the BCrypt hash
     */
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Checks if a plaintext password matches a BCrypt hash.
     * 
     * @param rawPassword     the plaintext password to check
     * @param encodedPassword the BCrypt hash to compare against
     * @return true if the password matches
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
