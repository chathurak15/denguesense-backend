package com.zeylex.denguesense.service;

import java.util.Map;

public interface TelegramInboxService {

    void handleUpdate(Map<String, Object> payload);
}
