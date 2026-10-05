package com.yourteam.lostfound.config;

import com.yourteam.lostfound.util.FileUploadUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:${file.upload-dir:./uploads}}")
    private String uploadDir;

    @PostConstruct
    public void initUploadDirectory() {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            FileUploadUtil.setUploadDir(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize uploads directory at " + uploadPath, e);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize uploads directory", e);
        }

        // uploadPath.toFile().toURI().toString() creates a valid "file:/C:/..." string
        // while properly handling the space in "3D Objects"
        String location = uploadPath.toFile().toURI().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}