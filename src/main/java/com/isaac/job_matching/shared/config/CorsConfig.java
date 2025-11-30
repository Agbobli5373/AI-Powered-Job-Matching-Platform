package com.isaac.job_matching.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 * 
 * <p>
 * Configures allowed origins, methods, and headers for cross-origin requests.
 * Binds to {@code app.cors.*} properties in application.yaml.
 */
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class CorsConfig {

    /**
     * Allowed origins for CORS requests.
     * Default: http://localhost:3000 (React development server)
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    /**
     * Allowed HTTP methods.
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    /**
     * Allowed request headers.
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * Headers exposed to the browser.
     */
    private List<String> exposedHeaders = List.of("Authorization", "Content-Disposition");

    /**
     * Whether credentials (cookies, authorization headers) are allowed.
     */
    private boolean allowCredentials = true;

    /**
     * How long the browser should cache the CORS preflight response (in seconds).
     */
    private long maxAge = 3600;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Creates a CORS configuration source bean.
     * 
     * <p>
     * This bean is automatically picked up by Spring Security
     * when CORS is enabled via {@code cors(Customizer.withDefaults())}.
     * 
     * @return CorsConfigurationSource with configured settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setExposedHeaders(exposedHeaders);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
