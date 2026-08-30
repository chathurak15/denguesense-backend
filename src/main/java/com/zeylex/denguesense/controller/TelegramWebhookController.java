package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.service.TelegramInboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final TelegramInboxService telegramInboxService;

    public TelegramWebhookController(TelegramInboxService telegramInboxService) {
        this.telegramInboxService = telegramInboxService;
    }

    @PostMapping("/telegram/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        try {
            telegramInboxService.handleUpdate(payload);
        } catch (Exception ex) {
            log.warn("Ignoring malformed Telegram webhook payload: {}", ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
