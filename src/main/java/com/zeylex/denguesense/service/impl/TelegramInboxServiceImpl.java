package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.TelegramRegistration;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.repo.TelegramRegistrationRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.ClusterQueryService;
import com.zeylex.denguesense.service.TelegramClient;
import com.zeylex.denguesense.service.TelegramConnectService;
import com.zeylex.denguesense.service.TelegramInboxService;
import com.zeylex.denguesense.util.TelegramAlertMessages;
import com.zeylex.denguesense.util.TelegramCommandParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TelegramInboxServiceImpl implements TelegramInboxService {

    private static final Logger log = LoggerFactory.getLogger(TelegramInboxServiceImpl.class);

    private final UserRepo userRepo;
    private final TelegramRegistrationRepo telegramRegistrationRepo;
    private final TelegramConnectService telegramConnectService;
    private final ClusterQueryService clusterQueryService;
    private final TelegramClient telegramClient;
    private final String frontendBaseUrl;

    public TelegramInboxServiceImpl(UserRepo userRepo,
                                    TelegramRegistrationRepo telegramRegistrationRepo,
                                    TelegramConnectService telegramConnectService,
                                    ClusterQueryService clusterQueryService,
                                    TelegramClient telegramClient,
                                    @Value("${denguesense.frontend.base.url:http://localhost:3000}") String frontendBaseUrl) {
        this.userRepo = userRepo;
        this.telegramRegistrationRepo = telegramRegistrationRepo;
        this.telegramConnectService = telegramConnectService;
        this.clusterQueryService = clusterQueryService;
        this.telegramClient = telegramClient;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void handleUpdate(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            log.warn("Telegram webhook received empty payload");
            return;
        }

        Map<String, Object> callback = asMap(payload.get("callback_query"));
        if (callback != null) {
            handleCallback(callback);
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
        handleMessage(chatId, text);
    }

    private void handleCallback(Map<String, Object> callback) {
        String callbackId = stringify(callback.get("id"));
        telegramClient.answerCallbackQuery(callbackId);

        Map<String, Object> message = asMap(callback.get("message"));
        Map<String, Object> chat = message == null ? null : asMap(message.get("chat"));
        String chatId = chat == null ? null : stringify(chat.get("id"));
        String data = stringify(callback.get("data"));
        if (chatId == null || data == null) {
            log.warn("Telegram callback missing chat.id or data");
            return;
        }

        Optional<User> officer = findOfficer(chatId);
        if (officer.isEmpty()) {
            sendNotConnected(chatId);
            return;
        }

        if (TelegramAlertMessages.CALLBACK_LIVE.equals(data)) {
            sendLiveClusters(chatId, officer.get());
            return;
        }
        if (TelegramAlertMessages.CALLBACK_HELP.equals(data)) {
            sendHelp(chatId, true);
            return;
        }
        String clusterId = TelegramAlertMessages.quickViewCallbackData(data);
        if (clusterId != null) {
            sendQuickView(chatId, officer.get(), clusterId);
            return;
        }
        log.debug("Ignoring unknown Telegram callback data={} chatId={}", data, chatId);
    }

    private void handleMessage(String chatId, String text) {
        TelegramCommandParser.Command command = TelegramCommandParser.classify(text);
        Optional<User> officer = findOfficer(chatId);

        if (command == TelegramCommandParser.Command.CONNECT) {
            handleConnect(chatId, text, officer);
            return;
        }
        if (officer.isEmpty()) {
            if (command != TelegramCommandParser.Command.UNKNOWN) {
                sendNotConnected(chatId);
            } else {
                log.debug("Ignoring non-connect Telegram message from chatId={}", chatId);
            }
            return;
        }

        switch (command) {
            case CLUSTERS -> sendLiveClusters(chatId, officer.get());
            case HELP -> sendHelp(chatId, true);
            default -> sendHelp(chatId, true);
        }
    }

    private void handleConnect(String chatId, String text, Optional<User> alreadyLinked) {
        String code = TelegramCommandParser.extractCode(text);
        if (code == null || code.isBlank()) {
            if (alreadyLinked.isPresent()) {
                sendHelp(chatId, true);
            } else {
                telegramConnectService.sendWelcomeHelp(chatId);
            }
            return;
        }

        User user = userRepo.findByTelegramRegistrationCode(code.trim()).orElse(null);
        if (user == null) {
            log.warn("Telegram connect code did not match any user (chatId={})", chatId);
            telegramConnectService.sendWelcomeHelp(chatId);
            return;
        }
        telegramConnectService.bindChat(user, chatId);
    }

    private void sendLiveClusters(String chatId, User officer) {
        try {
            List<ClusterResponseDTO> clusters = clusterQueryService.listLive(officer.getEmail(), null);
            String districtName = officer.getDistrict() == null ? null : officer.getDistrict().getName();
            telegramClient.sendHtml(
                    chatId,
                    TelegramAlertMessages.liveClusters(districtName, clusters, frontendBaseUrl),
                    TelegramAlertMessages.liveClustersKeyboard(clusters, frontendBaseUrl));
        } catch (Exception ex) {
            log.warn("Could not list live clusters for Telegram chatId={}: {}", chatId, ex.getMessage());
            sendHelp(chatId, true);
        }
    }

    private void sendQuickView(String chatId, User officer, String clusterIdText) {
        Long clusterId;
        try {
            clusterId = Long.valueOf(clusterIdText);
        } catch (NumberFormatException ex) {
            telegramClient.sendHtml(chatId, TelegramAlertMessages.clusterNotFound(), null);
            return;
        }
        try {
            ClusterResponseDTO cluster = clusterQueryService.getById(officer.getEmail(), clusterId);
            telegramClient.sendHtml(
                    chatId,
                    TelegramAlertMessages.clusterQuickView(cluster, frontendBaseUrl),
                    TelegramAlertMessages.clusterViewKeyboard(cluster, frontendBaseUrl));
        } catch (NotFoundException ex) {
            telegramClient.sendHtml(chatId, TelegramAlertMessages.clusterNotFound(), null);
        } catch (Exception ex) {
            log.warn("Could not load cluster {} for Telegram chatId={}: {}", clusterId, chatId, ex.getMessage());
            telegramClient.sendHtml(chatId, TelegramAlertMessages.clusterNotFound(), null);
        }
    }

    private void sendHelp(String chatId, boolean connected) {
        Map<String, Object> keyboard = connected ? TelegramAlertMessages.navReplyKeyboard() : null;
        telegramClient.sendHtml(chatId, TelegramAlertMessages.help(connected), keyboard);
    }

    private void sendNotConnected(String chatId) {
        telegramClient.sendHtml(chatId, TelegramAlertMessages.notConnected(), null);
    }

    private Optional<User> findOfficer(String chatId) {
        return telegramRegistrationRepo.findByChatIdWithUser(chatId)
                .map(TelegramRegistration::getUser);
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
