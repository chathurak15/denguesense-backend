package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.ai.ClassifyRequestDTO;
import com.zeylex.denguesense.dto.ai.ClassifyResponseDTO;
import com.zeylex.denguesense.exception.AiServiceException;
import com.zeylex.denguesense.service.AiClassificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
@Service
public class AiClassificationServiceImpl implements AiClassificationService {

    private static final Logger log = LoggerFactory.getLogger(AiClassificationServiceImpl.class);

    private final WebClient aiServiceWebClient;

    public AiClassificationServiceImpl(WebClient aiServiceWebClient) {
        this.aiServiceWebClient = aiServiceWebClient;
    }

    @Override
    public ClassifyResponseDTO classify(String imageUrl) {
        log.info("Requesting CNN classification for imageUrl={}", imageUrl);
        try {
            ClassifyResponseDTO result = callClassify(imageUrl);
            log.info("CNN classification result: riskLabel={}, confidenceScore={} for imageUrl={}",
                    result.riskLabel(), result.confidenceScore(), imageUrl);
            return result;
        } catch (WebClientRequestException ex) {
            log.warn("AI service unreachable on first attempt ({}), retrying once...", ex.getMessage());
            try {
                ClassifyResponseDTO result = callClassify(imageUrl);
                log.info("CNN classification succeeded on retry: riskLabel={}, confidenceScore={} for imageUrl={}",
                        result.riskLabel(), result.confidenceScore(), imageUrl);
                return result;
            } catch (WebClientRequestException retryEx) {
                throw new AiServiceException(
                        "AI classification service is unreachable after retry: " + retryEx.getMessage(), retryEx);
            } catch (WebClientResponseException retryRespEx) {
                throw new AiServiceException(
                        "AI classification service returned HTTP " + retryRespEx.getStatusCode()
                                + " on retry: " + retryRespEx.getResponseBodyAsString(), retryRespEx);
            }
        } catch (WebClientResponseException ex) {
            throw new AiServiceException(
                    "AI classification service returned HTTP " + ex.getStatusCode()
                            + ": " + ex.getResponseBodyAsString(), ex);
        }
    }

    private ClassifyResponseDTO callClassify(String imageUrl) {
        return aiServiceWebClient.post()
                .uri("/classify")
                .bodyValue(new ClassifyRequestDTO(imageUrl))
                .retrieve()
                .bodyToMono(ClassifyResponseDTO.class)
                .block();
    }
}
