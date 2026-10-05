package com.yourteam.lostfound.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/**
 * Storage service abstraction supporting both local filesystem storage
 * and cloud storage providers (such as Cloudinary).
 */
public interface FileStorageService {

    /**
     * Stores the uploaded file and returns its resolvable URL or storage path.
     * For cloud providers, returns the complete HTTPS URL (e.g. https://res.cloudinary.com/...).
     * For local storage, returns the relative URL (e.g. /uploads/<uuid>.<ext>).
     *
     * @param file the multipart file to store
     * @return the public URL or relative path
     * @throws IOException if saving fails
     */
    String storeFile(MultipartFile file) throws IOException;

    /**
     * Indicates whether the storage provider stores files in the cloud.
     */
    boolean isCloudStorage();
}
