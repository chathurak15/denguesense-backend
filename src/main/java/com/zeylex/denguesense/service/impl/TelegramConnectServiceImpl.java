package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.responseDTO.TelegramAlertStatusDTO;
import com.zeylex.denguesense.dto.responseDTO.UserResponseDTO;
import com.zeylex.denguesense.model.TelegramRegistration;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.RoleType;
import com.zeylex.denguesense.repo.TelegramRegistrationRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.TelegramClient;
import com.zeylex.denguesense.service.TelegramConnectService;
import com.zeylex.denguesense.util.TelegramAlertMessages;
import com.zeylex.denguesense.util.TelegramCommandParser;
import com.zeylex.denguesense.util.TelegramRegistrationCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class TelegramConnectServiceImpl implements TelegramConnectService {

    private static final Logger log = LoggerFactory.getLogger(TelegramConnectServiceImpl.class);

    static final String CONNECTED_MESSAGE = TelegramAlertMessages.connectedConfirmation();
    static final String START_HELP_MESSAGE = TelegramAlertMessages.help(false);

    private final UserRepo userRepo;
    private final TelegramRegistrationRepo telegramRegistrationRepo;
    private final TelegramClient telegramClient;
    private final WebClient telegramWebClient;
    private final String botToken;
    private final String botUsername;

    public TelegramConnectServiceImpl(UserRepo userRepo,
                                      TelegramRegistrationRepo telegramRegistrationRepo,
                                      TelegramClient telegramClient,
                                      @Qualifier("telegramWebClient") WebClient telegramWebClient,
                                      @Value("${telegram.bot.token:}") String botToken,
                                      @Value("${telegram.bot.username:denguesensebot}") String botUsername) {
        this.userRepo = userRepo;
        this.telegramRegistrationRepo = telegramRegistrationRepo;
        this.telegramClient = telegramClient;
        this.telegramWebClient = telegramWebClient;
        this.botToken = botToken;
        this.botUsername = stripAt(botUsername);
    }

    @Override
    public void assignCodeIfNeeded(User user) {
        if (user == null || user.getRole() != RoleType.PHI) {
            return;
        }
        String existing = user.getTelegramRegistrationCode();
        if (existing != null && !existing.isBlank()) {
            return;
        }
        String code;
        do {
            code = TelegramRegistrationCodes.generate();
        } while (userRepo.findByTelegramRegistrationCode(code).isPresent());
        user.setTelegramRegistrationCode(code);
    }

    @Override
    @Transactional
    public TelegramAlertStatusDTO statusFor(User user) {
        if (user == null) {
            return new TelegramAlertStatusDTO(false, null, botUsername);
        }
        String before = user.getTelegramRegistrationCode();
        assignCodeIfNeeded(user);
        boolean assigned = (before == null || before.isBlank()) && user.getTelegramRegistrationCode() != null;
        if (assigned && user.getId() != null) {
            userRepo.save(user);
        }
        return toStatus(user);
    }

    @Override
    public void applyTo(User user, UserResponseDTO dto) {
        if (dto == null) {
            return;
        }
        TelegramAlertStatusDTO status = toStatus(user);
        dto.setTelegramConnected(status.isConnected());
        dto.setTelegramConnectUrl(status.getConnectUrl());
    }

    @Override
    @Transactional
    public void bindChat(User user, String chatId) {
        if (user == null || chatId == null || chatId.isBlank()) {
            return;
        }
        Long districtId = user.getDistrict() == null ? null : user.getDistrict().getId();
        TelegramRegistration registration = telegramRegistrationRepo.findByUser_Id(user.getId())
                .orElseGet(TelegramRegistration::new);
        registration.setUser(user);
        registration.setChatId(chatId);
        registration.setDistrictId(districtId);
        telegramRegistrationRepo.save(registration);
        log.info("Telegram chat bound userId={} chatId={} districtId={}", user.getId(), chatId, districtId);
        try {
            telegramClient.sendHtml(chatId, CONNECTED_MESSAGE, TelegramAlertMessages.navReplyKeyboard());
        } catch (Exception ex) {
            log.warn("Could not send Telegram connect confirmation to chatId={}: {}", chatId, ex.getMessage());
        }
    }

    @Override
    @Transactional
    public TelegramAlertStatusDTO syncFromTelegram(User user) {
        TelegramAlertStatusDTO current = statusFor(user);
        if (current.isConnected() || user == null || user.getTelegramRegistrationCode() == null) {
            return current;
        }
        if (botToken == null || botToken.isBlank()) {
            return current;
        }

        String expected = user.getTelegramRegistrationCode().trim();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = telegramWebClient.get()
                    .uri("/bot{token}/getUpdates", botToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (body == null || !Boolean.TRUE.equals(body.get("ok"))) {
                return statusFor(user);
            }
            Object result = body.get("result");
            if (!(result instanceof List<?> updates)) {
                return statusFor(user);
            }
            for (Object update : updates) {
                if (!(update instanceof Map<?, ?> updateMap)) {
                    continue;
                }
                Object messageObj = updateMap.get("message");
                if (!(messageObj instanceof Map<?, ?> message)) {
                    continue;
                }
                String text = message.get("text") == null ? null : String.valueOf(message.get("text"));
                String code = TelegramCommandParser.extractCode(text);
                if (code == null || !expected.equalsIgnoreCase(code.trim())) {
                    continue;
                }
                Object chatObj = message.get("chat");
                if (!(chatObj instanceof Map<?, ?> chat)) {
                    continue;
                }
                Object chatId = chat.get("id");
                if (chatId == null) {
                    continue;
                }
                bindChat(user, String.valueOf(chatId));
                break;
            }
        } catch (WebClientResponseException ex) {
            log.debug("Telegram getUpdates unavailable (webhook may be set): {}", ex.getStatusCode());
        } catch (Exception ex) {
            log.warn("Telegram getUpdates sync failed: {}", ex.getMessage());
        }
        return statusFor(user);
    }

    @Override
    public void sendDirectMessage(String chatId, String text) {
        telegramClient.sendHtml(chatId, text, null);
    }

    @Override
    public void sendWelcomeHelp(String chatId) {
        telegramClient.sendHtml(chatId, START_HELP_MESSAGE, null);
    }

    private TelegramAlertStatusDTO toStatus(User user) {
        boolean connected = user != null
                && user.getId() != null
                && telegramRegistrationRepo.findByUser_Id(user.getId()).isPresent();
        String connectUrl = null;
        if (user != null && user.getRole() == RoleType.PHI) {
            String code = user.getTelegramRegistrationCode();
            if (code != null && !code.isBlank() && botUsername != null && !botUsername.isBlank()) {
                connectUrl = "https://t.me/" + botUsername + "?start=" + code.trim();
            }
        }
        return new TelegramAlertStatusDTO(connected, connectUrl, botUsername);
    }

    private static String stripAt(String username) {
        if (username == null) {
            return "denguesensebot";
        }
        String trimmed = username.trim();
        return trimmed.startsWith("@") ? trimmed.substring(1) : trimmed;
    }
}
