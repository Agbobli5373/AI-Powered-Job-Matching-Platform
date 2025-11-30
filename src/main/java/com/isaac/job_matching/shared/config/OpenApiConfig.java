package com.isaac.job_matching.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger documentation configuration.
 * 
 * <p>Provides interactive API documentation at:
 * <ul>
 *   <li>Swagger UI: /swagger-ui.html</li>
 *   <li>OpenAPI JSON: /v3/api-docs</li>
 *   <li>OpenAPI YAML: /v3/api-docs.yaml</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.jobmatching.com")
                    .description("Production Server")
            ))
            .tags(List.of(
                new Tag().name("Authentication").description("User registration, login, and token management"),
                new Tag().name("Users").description("User account management"),
                new Tag().name("Profiles").description("Job seeker profile management"),
                new Tag().name("Companies").description("Employer company management"),
                new Tag().name("Jobs").description("Job posting management"),
                new Tag().name("Applications").description("Job application management"),
                new Tag().name("Search").description("Job and candidate search"),
                new Tag().name("Matching").description("AI-powered job and candidate matching"),
                new Tag().name("Resumes").description("Resume upload and AI parsing"),
                new Tag().name("Messaging").description("In-platform messaging"),
                new Tag().name("Notifications").description("User notifications and alerts"),
                new Tag().name("Analytics").description("Performance metrics and analytics"),
                new Tag().name("Admin").description("Platform administration")
            ))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Authorization header using the Bearer scheme. " +
                        "Enter your token in the text input below. " +
                        "Example: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'")
                )
            );
    }

    private Info apiInfo() {
        return new Info()
            .title("AI-Powered Job Matching Platform API")
            .version("1.0.0")
            .description("""
                ## Overview
                
                The AI-Powered Job Matching Platform API provides endpoints for:
                
                - **Job Seekers**: Register, create profiles, search jobs, and apply
                - **Employers**: Post jobs, search candidates, and manage applications
                - **AI Matching**: Get personalized job/candidate recommendations
                - **Communication**: In-platform messaging and notifications
                
                ## Authentication
                
                Most endpoints require JWT authentication. To authenticate:
                
                1. Register a new account via `POST /api/auth/register`
                2. Login via `POST /api/auth/login` to receive access and refresh tokens
                3. Include the access token in the `Authorization` header: `Bearer <token>`
                4. Refresh tokens via `POST /api/auth/refresh` before expiry
                
                ## Rate Limiting
                
                - Standard endpoints: 100 requests/minute
                - Search endpoints: 30 requests/minute
                - AI matching endpoints: 10 requests/minute
                
                ## Error Responses
                
                All errors follow a standard format:
                ```json
                {
                  "status": 400,
                  "error": "Bad Request",
                  "message": "Validation failed",
                  "path": "/api/profiles",
                  "timestamp": "2025-11-30T10:00:00Z",
                  "errors": {
                    "email": "must be a valid email address"
                  }
                }
                ```
                """)
            .contact(new Contact()
                .name("Job Matching Platform Team")
                .email("support@jobmatching.com")
                .url("https://jobmatching.com"))
            .license(new License()
                .name("Proprietary")
                .url("https://jobmatching.com/terms"));
    }
}
