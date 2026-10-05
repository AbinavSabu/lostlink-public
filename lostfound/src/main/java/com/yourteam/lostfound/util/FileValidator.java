package com.yourteam.lostfound.util;

import com.yourteam.lostfound.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

public class FileValidator {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    private FileValidator() {}

    public static void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 5MB");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !hasValidExtension(fileName)) {
            throw new BadRequestException("Invalid file type. Only JPG, JPEG, PNG, and WEBP are allowed");
        }
    }

    private static boolean hasValidExtension(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(lowerCaseName::endsWith);
    }
}