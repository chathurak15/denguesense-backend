package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.service.TelegramClient;
import com.zeylex.denguesense.util.TelegramHtml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TelegramClientImpl implements TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClientImpl.class);

    private final WebClient telegramWebClient;
    private final String botToken;

    public TelegramClientImpl(@Qualifier("telegramWebClient") WebClient telegramWebClient,
                              @Value("${telegram.bot.token:}") String botToken) {
        this.telegramWebClient = telegramWebClient;
        this.botToken = botToken;
    }

    @Override
    public void sendHtml(String chatId, String html, Map<String, Object> replyMarkup) {
        try {
            postSendMessage(chatId, html, "HTML", replyMarkup, true);
        } catch (Exception ex) {
            log.warn("Rich Telegram send failed for chatId={}: {}. Retrying as plain text.",
                    chatId, ex.getMessage());
            postSendMessage(chatId, TelegramHtml.stripTags(html), null, null, true);
        }
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId) {
        if (callbackQueryId == null || callbackQueryId.isBlank()) {
            return;
        }
        requireToken();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callback_query_id", callbackQueryId);
        try {
            post("/bot{token}/answerCallbackQuery", payload);
        } catch (Exception ex) {
            log.warn("Telegram answerCallbackQuery failed: {}", ex.getMessage());
        }
    }

    private void postSendMessage(String chatId,
                                 String text,
                                 String parseMode,
                                 Map<String, Object> replyMarkup,
                                 boolean disablePreview) {
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException("chatId is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("message text is required");
        }
        requireToken();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);
        if (parseMode != null && !parseMode.isBlank()) {
            payload.put("parse_mode", parseMode);
        }
        payload.put("disable_web_page_preview", disablePreview);
        if (replyMarkup != null && !replyMarkup.isEmpty()) {
            payload.put("reply_markup", replyMarkup);
        }
        post("/bot{token}/sendMessage", payload);
    }

    private void post(String uri, Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = telegramWebClient.post()
                .uri(uri, botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
            String description = response == null ? "empty Telegram response" : String.valueOf(response.get("description"));
            throw new IllegalStateException("Telegram API failed: " + description);
        }
    }

    private void requireToken() {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("telegram.bot.token is not configured");
        }
    }
}
