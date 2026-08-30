package com.zeylex.denguesense.service;

import java.util.Map;

public interface TelegramClient {

    void sendHtml(String chatId, String html, Map<String, Object> replyMarkup);

    void answerCallbackQuery(String callbackQueryId);
}
