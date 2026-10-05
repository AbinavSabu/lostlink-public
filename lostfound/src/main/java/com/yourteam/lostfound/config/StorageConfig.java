package com.yourteam.lostfound.config;

import com.yourteam.lostfound.service.FileStorageService;
import com.yourteam.lostfound.service.impl.CloudinaryFileStorageService;
import com.yourteam.lostfound.service.impl.LocalFileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${app.storage.provider:${STORAGE_PROVIDER:local}}")
    private String storageProvider;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    @Primary
    public FileStorageService fileStorageService() {
        boolean hasCloudinaryCreds = cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();

        if ("cloudinary".equalsIgnoreCase(storageProvider) || (hasCloudinaryCreds && !"local".equalsIgnoreCase(storageProvider))) {
            if (hasCloudinaryCreds) {
                log.info("Initialized Cloudinary file storage provider for cloud name: {}", cloudName);
                return new CloudinaryFileStorageService(cloudName, apiKey, apiSecret);
            } else {
                log.warn("Cloudinary storage provider was selected, but credentials are missing! Falling back to local filesystem storage.");
            }
        }

        log.info("Initialized Local filesystem storage provider (uploads directory)");
        return new LocalFileStorageService();
    }
}
