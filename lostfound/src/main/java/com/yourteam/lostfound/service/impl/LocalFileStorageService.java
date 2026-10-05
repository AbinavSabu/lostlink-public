package com.yourteam.lostfound.service.impl;

import com.yourteam.lostfound.service.FileStorageService;
import com.yourteam.lostfound.util.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Local filesystem implementation of FileStorageService.
 * Used for development, local demos, and JUnit integration testing.
 */
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        log.info("Storing file [{}] locally to uploads directory", file.getOriginalFilename());
        return FileUploadUtil.saveFile(file);
    }

    @Override
    public boolean isCloudStorage() {
        return false;
    }
}
