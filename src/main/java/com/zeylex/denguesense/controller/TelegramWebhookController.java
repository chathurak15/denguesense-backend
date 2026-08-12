package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.model.TelegramRegistration;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.repo.TelegramRegistrationRepo;
import com.zeylex.denguesense.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final UserRepo userRepo;
    private final TelegramRegistrationRepo telegramRegistrationRepo;

    public TelegramWebhookController(UserRepo userRepo, TelegramRegistrationRepo telegramRegistrationRepo) {
        this.userRepo = userRepo;
        this.telegramRegistrationRepo = telegramRegistrationRepo;
    }

    @PostMapping("/telegram/webhook")
    @Transactional
    public ResponseEntity<Void> handleWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        try {
            processUpdate(payload);
        } catch (Exception ex) {
            log.warn("Ignoring malformed Telegram webhook payload: {}", ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    private void processUpdate(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            log.warn("Telegram webhook received empty payload");
            return;
        }

        Map<String, Object> message = asMap(payload.get("message"));
        if (message == null) {
            log.debug("Telegram webhook had no message object (update_id={})", payload.get("update_id"));
            return;
        }

        Map<String, Object> chat = asMap(message.get("chat"));
        String chatId = chat == null ? null : stringify(chat.get("id"));
        String text = stringify(message.get("text"));

        if (chatId == null || text == null) {
            log.warn("Telegram webhook missing chat.id or text (update_id={})", payload.get("update_id"));
            return;
        }

        if (!text.startsWith("/register")) {
            log.debug("Ignoring non-register Telegram message from chatId={}", chatId);
            return;
        }

        String code = extractRegistrationCode(text);
        if (code == null || code.isBlank()) {
            log.warn("Telegram /register from chatId={} had no code", chatId);
            return;
        }

        User user = userRepo.findByTelegramRegistrationCode(code.trim()).orElse(null);
        if (user == null) {
            log.warn("Telegram /register code did not match any user (chatId={})", chatId);
            return;
        }

        Long districtId = user.getDistrict() == null ? null : user.getDistrict().getId();
        TelegramRegistration registration = telegramRegistrationRepo.findByUser_Id(user.getId())
                .orElseGet(TelegramRegistration::new);
        registration.setUser(user);
        registration.setChatId(chatId);
        registration.setDistrictId(districtId);
        telegramRegistrationRepo.save(registration);

        log.info("Telegram registration saved for userId={} chatId={} districtId={}",
                user.getId(), chatId, districtId);
    }

    private static String extractRegistrationCode(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        return parts[1];
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private static String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
