package com.yourteam.lostfound.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUploadUtil {

    private static volatile String uploadDir = "./uploads";

    private FileUploadUtil() {}

    public static void setUploadDir(String dir) {
        if (dir != null && !dir.trim().isEmpty()) {
            uploadDir = dir.trim();
        }
    }

    public static String getUploadDir() {
        return uploadDir;
    }

    public static String saveFile(MultipartFile multipartFile) throws IOException {
        FileValidator.validateImageFile(multipartFile);

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = multipartFile.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + uniqueFileName;
        }
    }
}