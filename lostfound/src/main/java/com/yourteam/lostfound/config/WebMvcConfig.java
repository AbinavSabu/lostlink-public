package com.yourteam.lostfound.config;

import com.yourteam.lostfound.util.FileUploadUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * WebMvcConfig manages static resource handlers and upload directory initialization.
 *
 * Cloud-native resilience:
 * When STORAGE_PROVIDER is set to "cloudinary" (or Cloudinary credentials are provided),
 * all local filesystem directory creation and /uploads/** resource handler registrations
 * are completely bypassed. This prevents java.nio.file.AccessDeniedException on read-only
 * container environments such as Render, Heroku, and AWS ECS.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    @Value("${app.upload.dir:${file.upload-dir:./uploads}}")
    private String uploadDir;

    @Value("${app.storage.provider:${STORAGE_PROVIDER:local}}")
    private String storageProvider;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    /**
     * Determines whether cloud storage (Cloudinary) is active.
     */
    private boolean isCloudStorageEnabled() {
        boolean hasCloudinaryCreds = cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();

        return "cloudinary".equalsIgnoreCase(storageProvider)
                || (hasCloudinaryCreds && !"local".equalsIgnoreCase(storageProvider));
    }

    @PostConstruct
    public void initUploadDirectory() {
        // If Cloudinary is enabled, do NOT create local directories
        if (isCloudStorageEnabled()) {
            log.info("Cloud storage provider is active [{}]. Skipping local filesystem upload directory initialization.", storageProvider);
            return;
        }

        // Local storage mode: initialize upload directory safely
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Initialized local uploads directory at: {}", uploadPath);
            }
            FileUploadUtil.setUploadDir(uploadDir);
        } catch (IOException e) {
            log.warn("Could not create local upload directory at {} ({}). File uploads may fail if local storage is used.",
                    uploadPath, e.getMessage());
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // When Cloudinary is active, all uploads reside in the cloud and are served via HTTPS.
        // Do NOT create directories or register local file resource handlers.
        if (isCloudStorageEnabled()) {
            log.info("Cloud storage provider is active [{}]. Bypassing local /uploads/** static resource handler registration.", storageProvider);
            return;
        }

        // Local storage mode: register /uploads/** only if directory exists or can be resolved
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            log.warn("Local uploads directory does not exist at {} and could not be created ({}). Skipping /uploads/** mapping.",
                    uploadPath, e.getMessage());
            return;
        }

        String location = uploadPath.toFile().toURI().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        log.info("Registering local static resource handler for /uploads/** -> {}", location);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    @Value("${app.cors.allowed-origins:${FRONTEND_URL:}}")
    private String allowedCorsOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://lostlink-public-frontend-3znt.vercel.app",
                "https://*.vercel.app"
        ));

        if (allowedCorsOrigins != null && !allowedCorsOrigins.isBlank()) {
            Arrays.stream(allowedCorsOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(origins::add);
        }

        registry.addMapping("/**")
                .allowedOriginPatterns(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}