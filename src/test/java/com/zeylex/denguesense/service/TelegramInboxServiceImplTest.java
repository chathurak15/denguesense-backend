package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.TelegramRegistration;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.repo.TelegramRegistrationRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.impl.TelegramInboxServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramInboxService — navigation and quick view")
class TelegramInboxServiceImplTest {

    @Mock private UserRepo userRepo;
    @Mock private TelegramRegistrationRepo telegramRegistrationRepo;
    @Mock private TelegramConnectService telegramConnectService;
    @Mock private ClusterQueryService clusterQueryService;
    @Mock private TelegramClient telegramClient;

    private TelegramInboxServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TelegramInboxServiceImpl(
                userRepo,
                telegramRegistrationRepo,
                telegramConnectService,
                clusterQueryService,
                telegramClient,
                "https://app.denguesense.lk");
    }

    @Test
    void liveClustersButton_sendsDistrictHotspots() {
        when(telegramRegistrationRepo.findByChatIdWithUser("137")).thenReturn(Optional.of(registration()));
        when(clusterQueryService.listLive("phi@health.gov.lk", null)).thenReturn(List.of(clusterSummary()));

        service.handleUpdate(message("137", "🔴 Live clusters"));

        verify(telegramClient).sendHtml(eq("137"), contains("Live dengue clusters"), any());
        verify(telegramClient).sendHtml(eq("137"), contains("Colombo"), any());
    }

    @Test
    void quickViewCallback_sendsClusterSummary() {
        when(telegramRegistrationRepo.findByChatIdWithUser("137")).thenReturn(Optional.of(registration()));
        when(clusterQueryService.getById("phi@health.gov.lk", 12L)).thenReturn(clusterSummary());

        service.handleUpdate(callback("137", "qv:12", "99"));

        verify(telegramClient).answerCallbackQuery("99");
        verify(telegramClient).sendHtml(eq("137"), contains("Cluster #12"), any());
        verify(telegramClient).sendHtml(eq("137"), contains("quick view"), any());
    }

    @Test
    void unknownUnregisteredMessage_isIgnored() {
        when(telegramRegistrationRepo.findByChatIdWithUser("137")).thenReturn(Optional.empty());

        service.handleUpdate(message("137", "hello"));

        verify(telegramClient, never()).sendHtml(any(), any(), any());
        verify(telegramConnectService, never()).sendWelcomeHelp(any());
    }

    private static TelegramRegistration registration() {
        District district = new District();
        district.setId(1L);
        district.setName("Colombo");
        User user = new User();
        user.setId(4L);
        user.setEmail("phi@health.gov.lk");
        user.setDistrict(district);
        TelegramRegistration registration = new TelegramRegistration();
        registration.setChatId("137");
        registration.setUser(user);
        return registration;
    }

    private static ClusterResponseDTO clusterSummary() {
        ClusterResponseDTO dto = new ClusterResponseDTO();
        dto.setId(12L);
        dto.setDistrictName("Colombo");
        dto.setReportCount(8);
        dto.setRisk("High");
        dto.setInsight("Prioritise PHI dispatch after recent rainfall.");
        dto.setLatitude(6.9271);
        dto.setLongitude(79.8612);
        return dto;
    }

    private static Map<String, Object> message(String chatId, String text) {
        Map<String, Object> chat = new LinkedHashMap<>();
        chat.put("id", chatId);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("chat", chat);
        message.put("text", text);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("update_id", 1);
        payload.put("message", message);
        return payload;
    }

    private static Map<String, Object> callback(String chatId, String data, String callbackId) {
        Map<String, Object> chat = new LinkedHashMap<>();
        chat.put("id", chatId);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("chat", chat);
        Map<String, Object> callback = new LinkedHashMap<>();
        callback.put("id", callbackId);
        callback.put("data", data);
        callback.put("message", message);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("update_id", 2);
        payload.put("callback_query", callback);
        return payload;
    }
}
