package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.ai.ClassifyResponseDTO;

public interface AiClassificationService {
    ClassifyResponseDTO classify(String imageUrl);
}
