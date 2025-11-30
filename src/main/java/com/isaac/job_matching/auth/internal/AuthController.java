package com.isaac.job_matching.auth.internal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isaac.job_matching.auth.AuthService;
import com.isaac.job_matching.auth.TokenPair;
import com.isaac.job_matching.user.User;
import com.isaac.job_matching.user.UserRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * REST controller for authentication endpoints.
 * 
 * <p>
 * All endpoints are publicly accessible (no authentication required).
 * 
 * <p>
 * Endpoints:
 * <ul>
 * <li>POST /api/auth/register - Register a new user</li>
 * <li>POST /api/auth/login - Authenticate and get tokens</li>
 * <li>POST /api/auth/refresh - Refresh access token</li>
 * <li>POST /api/auth/verify-email - Verify email address</li>
 * <li>POST /api/auth/forgot-password - Request password reset</li>
 * <li>POST /api/auth/reset-password - Reset password with token</li>
 * <li>POST /api/auth/logout - Revoke refresh token</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {

        private final AuthService authService;

        public AuthController(AuthService authService) {
                this.authService = authService;
        }

        @PostMapping("/register")
        @Operation(summary = "Register a new user", description = "Creates a new user account. Sends verification email.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "User created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input"),
                        @ApiResponse(responseCode = "409", description = "Email already registered")
        })
        public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
                UserRole role = UserRole.fromString(request.role());
                User user = authService.register(request.email(), request.password(), role);

                // Generate verification token (would be sent via email in production)
                String verificationToken = authService.generateVerificationToken(user.getId());

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(new RegisterResponse(
                                                user.getId(),
                                                user.getEmail(),
                                                "Registration successful. Please verify your email."));
        }

        @PostMapping("/login")
        @Operation(summary = "Authenticate user", description = "Validates credentials and returns access/refresh tokens")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Authentication successful"),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials or inactive account")
        })
        public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
                TokenPair tokens = authService.login(request.email(), request.password());

                return ResponseEntity.ok(TokenResponse.from(tokens, request.email()));
        }

        @PostMapping("/refresh")
        @Operation(summary = "Refresh access token", description = "Exchanges refresh token for new access/refresh tokens")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "New tokens issued"),
                        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
        })
        public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
                TokenPair tokens = authService.refreshToken(request.refreshToken());

                return ResponseEntity.ok(TokenResponse.from(tokens, null));
        }

        @PostMapping("/verify-email")
        @Operation(summary = "Verify email address", description = "Verifies email using the token sent during registration")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Email verified"),
                        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
        })
        public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
                authService.verifyEmail(request.token());

                return ResponseEntity.ok(new MessageResponse("Email verified successfully. You can now log in."));
        }

        @PostMapping("/forgot-password")
        @Operation(summary = "Request password reset", description = "Sends password reset email if account exists")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Reset email sent if account exists")
        })
        public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
                // Always return success to prevent email enumeration
                authService.initiatePasswordReset(request.email());

                return ResponseEntity.ok(new MessageResponse(
                                "If an account with that email exists, a password reset link has been sent."));
        }

        @PostMapping("/reset-password")
        @Operation(summary = "Reset password", description = "Resets password using the token from email")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Password reset successful"),
                        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
        })
        public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
                authService.resetPassword(request.token(), request.newPassword());

                return ResponseEntity.ok(new MessageResponse(
                                "Password has been reset. You can now log in with your new password."));
        }

        @PostMapping("/logout")
        @Operation(summary = "Logout", description = "Revokes the refresh token")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Logged out successfully")
        })
        public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshRequest request) {
                authService.logout(request.refreshToken());

                return ResponseEntity.ok(new MessageResponse("Logged out successfully."));
        }

        // Request/Response DTOs

        public record RegisterRequest(
                        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 255) String email,

                        @NotBlank(message = "Password is required") @Size(min = 8, max = 128, message = "Password must be 8-128 characters") String password,

                        @NotNull(message = "Role is required") String role) {
        }

        public record RegisterResponse(
                        UUID id,
                        String email,
                        String message) {
        }

        public record LoginRequest(
                        @NotBlank(message = "Email is required") @Email String email,

                        @NotBlank(message = "Password is required") String password) {
        }

        public record TokenResponse(
                        String accessToken,
                        String refreshToken,
                        Instant expiresAt,
                        UserInfo user) {

                public static TokenResponse from(TokenPair tokens, String email) {
                        return new TokenResponse(
                                        tokens.accessToken(),
                                        tokens.refreshToken(),
                                        tokens.expiresAt(),
                                        email != null ? new UserInfo(email) : null);
                }
        }

        public record UserInfo(String email) {
        }

        public record RefreshRequest(
                        @NotBlank(message = "Refresh token is required") String refreshToken) {
        }

        public record VerifyEmailRequest(
                        @NotBlank(message = "Token is required") String token) {
        }

        public record ForgotPasswordRequest(
                        @NotBlank(message = "Email is required") @Email String email) {
        }

        public record ResetPasswordRequest(
                        @NotBlank(message = "Token is required") String token,

                        @NotBlank(message = "New password is required") @Size(min = 8, max = 128) String newPassword) {
        }

        public record MessageResponse(String message) {
        }
}
