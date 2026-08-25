package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.TelegramAlertStatusDTO;
import com.zeylex.denguesense.dto.responseDTO.UserResponseDTO;
import com.zeylex.denguesense.model.User;

public interface TelegramConnectService {

    void assignCodeIfNeeded(User user);

    TelegramAlertStatusDTO statusFor(User user);

    void applyTo(User user, UserResponseDTO dto);

    void bindChat(User user, String chatId);

    TelegramAlertStatusDTO syncFromTelegram(User user);

    void sendDirectMessage(String chatId, String text);

    void sendWelcomeHelp(String chatId);
}
