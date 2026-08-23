package com.zeylex.denguesense.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    String uploadReportImage(MultipartFile file, String deviceUUID);
}