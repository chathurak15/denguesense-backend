package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;
import com.zeylex.denguesense.model.Notification;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.TelegramRegistration;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import com.zeylex.denguesense.model.enums.RoleType;
import com.zeylex.denguesense.repo.ReportClusterRepo;
import com.zeylex.denguesense.repo.TelegramRegistrationRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.impl.TelegramAlertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramAlertService — send vs cluster ALERTED bookkeeping")
class TelegramAlertServiceImplTest {

    @Mock private UserRepo userRepo;
    @Mock private TelegramRegistrationRepo telegramRegistrationRepo;
    @Mock private NotificationRecordService notificationRecordService;
    @Mock private ReportClusterRepo reportClusterRepo;
    @Mock private ClusterQueryService clusterQueryService;
    @Mock private TelegramClient telegramClient;

    private TelegramAlertServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TelegramAlertServiceImpl(
                userRepo,
                telegramRegistrationRepo,
                notificationRecordService,
                reportClusterRepo,
                clusterQueryService,
                telegramClient,
                "https://app.denguesense.lk");
    }

    @Test
    @DisplayName("Telegram delivered but ALERTED update fails → notification stays SENT, not FAILED")
    void telegramSent_alertedTransitionFails_doesNotMarkFailed() {
        ReportCluster cluster = cluster(2L);
        User phi = phi(4L);
        when(userRepo.findByRoleAndDistrict_Id(RoleType.PHI, 1L)).thenReturn(List.of(phi));
        when(clusterQueryService.getById("phi@health.gov.lk", 2L)).thenReturn(summary());

        TelegramRegistration registration = new TelegramRegistration();
        registration.setChatId("1374112634");
        when(telegramRegistrationRepo.findByUser_Id(4L)).thenReturn(Optional.of(registration));

        when(notificationRecordService.saveInNewTransaction(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(5L);
            return notification;
        });
        when(reportClusterRepo.updateStatusIfCurrent(2L, ClusterStatus.ACTIVE, ClusterStatus.ALERTED))
                .thenThrow(new DataIntegrityViolationException("could not execute statement"));

        service.sendClusterAlert(cluster);

        verify(notificationRecordService).markSent(5L);
        verify(notificationRecordService, never()).markFailed(eq(5L), anyString());
        verify(telegramClient).sendHtml(eq("1374112634"), anyString(), any());
    }

    @Test
    @DisplayName("Telegram API failure → notification is FAILED")
    void telegramApiFails_marksFailed() {
        ReportCluster cluster = cluster(2L);
        User phi = phi(4L);
        when(userRepo.findByRoleAndDistrict_Id(RoleType.PHI, 1L)).thenReturn(List.of(phi));

        TelegramRegistration registration = new TelegramRegistration();
        registration.setChatId("1374112634");
        when(telegramRegistrationRepo.findByUser_Id(4L)).thenReturn(Optional.of(registration));

        when(notificationRecordService.saveInNewTransaction(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(5L);
            return notification;
        });
        doThrow(new IllegalStateException("Telegram sendMessage failed"))
                .when(telegramClient).sendHtml(eq("1374112634"), anyString(), any());

        service.sendClusterAlert(cluster);

        verify(notificationRecordService).markFailed(eq(5L), anyString());
        verify(notificationRecordService, never()).markSent(5L);
        verify(reportClusterRepo, never()).updateStatusIfCurrent(any(), any(), any());
    }

    @Test
    @DisplayName("Alert includes hotspot copy plus cluster view and map buttons")
    @SuppressWarnings("unchecked")
    void sendClusterAlert_includesViewButtons() {
        ReportCluster cluster = cluster(2L);
        User phi = phi(4L);
        when(userRepo.findByRoleAndDistrict_Id(RoleType.PHI, 1L)).thenReturn(List.of(phi));
        when(clusterQueryService.getById("phi@health.gov.lk", 2L)).thenReturn(summary());

        TelegramRegistration registration = new TelegramRegistration();
        registration.setChatId("1374112634");
        when(telegramRegistrationRepo.findByUser_Id(4L)).thenReturn(Optional.of(registration));
        when(notificationRecordService.saveInNewTransaction(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(5L);
            return notification;
        });

        service.sendClusterAlert(cluster);

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> markup = ArgumentCaptor.forClass(Map.class);
        verify(telegramClient).sendHtml(eq("1374112634"), text.capture(), markup.capture());

        assertThat(text.getValue()).contains("DENGUE HOTSPOT ALERT");
        assertThat(text.getValue()).contains("Colombo");
        assertThat(text.getValue()).contains("View cluster");
        assertThat(text.getValue()).contains("https://app.denguesense.lk/phi/clusters/2");

        Map<String, Object> keyboard = markup.getValue();
        assertThat(keyboard).containsKey("inline_keyboard");
        String serialized = String.valueOf(keyboard);
        assertThat(serialized).contains("View cluster");
        assertThat(serialized).contains("/phi/clusters/2");
        assertThat(serialized).contains("Open map");
        assertThat(serialized).contains("Quick view");
        assertThat(serialized).contains("qv:2");
    }

    private static ReportCluster cluster(Long id) {
        ReportCluster cluster = new ReportCluster();
        cluster.setId(id);
        cluster.setDistrictId(1L);
        cluster.setStatus(ClusterStatus.ACTIVE);
        cluster.setReportCount(5);
        cluster.setDetectedAt(java.time.LocalDateTime.now());
        return cluster;
    }

    private static ClusterResponseDTO summary() {
        ClusterResponseDTO dto = new ClusterResponseDTO();
        dto.setId(2L);
        dto.setDistrictId(1L);
        dto.setDistrictName("Colombo");
        dto.setReportCount(5);
        dto.setRisk("High");
        dto.setInsight("AI classification flags a high-risk cluster on private premises.");
        dto.setLatitude(6.9271);
        dto.setLongitude(79.8612);
        dto.setDetectedAt(java.time.LocalDateTime.of(2026, 8, 29, 18, 42));
        return dto;
    }

    private static User phi(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("phi@health.gov.lk");
        user.setRole(RoleType.PHI);
        return user;
    }
}
