package com.zeylex.denguesense.service.impl;

import com.cloudinary.Cloudinary;
import com.zeylex.denguesense.exception.ImageUploadException;
import com.zeylex.denguesense.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryServiceImpl.class);
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadReportImage(MultipartFile file, String deviceUUID) {
        // File validation
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("Image file is required and must not be empty");
        }

        String contentType = file.getContentType();
        if ("application/octet-stream".equals(contentType)) {
            String name = file.getOriginalFilename();
            if (name != null) {
                if (name.endsWith(".jpg") || name.endsWith(".jpeg")) contentType = "image/jpeg";
                else if (name.endsWith(".png")) contentType = "image/png";
                else if (name.endsWith(".webp")) contentType = "image/webp";
            }
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImageUploadException(
                    "Image exceeds the 5 MB size limit (received " +
                    (file.getSize() / (1024 * 1024)) + " MB)");
        }

        // Folder: denguesense/reports/yyyy-MM (e.g. denguesense/reports/2025-08)
        // DeviceUUID is deliberately excluded from folder/public_id for citizen privacy.
        String folder = "denguesense/reports/" +
                YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Map<String, Object> uploadParams = Map.of(
                "folder",        folder,
                "resource_type", "image",
                "quality",       "auto:good",
                "fetch_format",  "auto",
                "width",         1024,
                "height",        1024,
                "crop",          "limit"
        );

        try {
            byte[] bytes = file.getBytes();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(bytes, uploadParams);

            String secureUrl = (String) result.get("secure_url");
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new ImageUploadException("Cloudinary returned no secure_url in the upload response");
            }
            log.info("Report image uploaded to Cloudinary: public_id={}", result.get("public_id"));
            return secureUrl;

        } catch (ImageUploadException e) {
            throw e;
        } catch (IOException e) {
            log.error("Cloudinary upload failed for device {}: {}", deviceUUID, e.getMessage(), e);
            throw new ImageUploadException(
                    "Image upload failed due to an I/O error. Please try again.", e);
        } catch (Exception e) {
            log.error("Unexpected error during Cloudinary upload for device {}: {}", deviceUUID, e.getMessage(), e);
            throw new ImageUploadException(
                    "Image upload failed. Please try again later.", e);
        }
    }
}