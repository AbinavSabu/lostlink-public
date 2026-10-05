package com.yourteam.lostfound.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.yourteam.lostfound.service.FileStorageService;
import com.yourteam.lostfound.util.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Cloudinary cloud-storage implementation of FileStorageService.
 * Used in public and production deployments (e.g. Render).
 */
public class CloudinaryFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryFileStorageService.class);

    private final Cloudinary cloudinary;

    public CloudinaryFileStorageService(String cloudName, String apiKey, String apiSecret) {
        if (cloudName == null || cloudName.isBlank() ||
            apiKey == null || apiKey.isBlank() ||
            apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalArgumentException("Cloudinary cloudName, apiKey, and apiSecret must not be empty");
        }
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        FileValidator.validateImageFile(file);

        String originalFilename = file.getOriginalFilename();
        log.info("Uploading file [{}] to Cloudinary...", originalFilename);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "lostlink/uploads",
                "resource_type", "image",
                "use_filename", false,
                "unique_filename", true
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Cloudinary upload successful. Public URL: {}", secureUrl);
        return secureUrl;
    }

    @Override
    public boolean isCloudStorage() {
        return true;
    }
}
