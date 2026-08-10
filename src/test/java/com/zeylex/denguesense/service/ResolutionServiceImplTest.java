//package com.zeylex.denguesense.service;
//
//import com.zeylex.denguesense.dto.requestDTO.ResolutionRequestDTO;
//import com.zeylex.denguesense.dto.responseDTO.ResolutionResponseDTO;
//import com.zeylex.denguesense.exception.DuplicationException;
//import com.zeylex.denguesense.exception.InvalidStateException;
//import com.zeylex.denguesense.exception.NotFoundException;
//import com.zeylex.denguesense.model.Report;
//import com.zeylex.denguesense.model.Resolution;
//import com.zeylex.denguesense.model.User;
//import com.zeylex.denguesense.model.enums.ReportStatus;
//import com.zeylex.denguesense.model.enums.ResolutionAction;
//import com.zeylex.denguesense.repo.ReportRepo;
//import com.zeylex.denguesense.repo.ResolutionRepo;
//import com.zeylex.denguesense.repo.UserRepo;
//import com.zeylex.denguesense.service.impl.ResolutionServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//@ExtendWith(MockitoExtension.class)
//class ResolutionServiceImplTest {
//
//    @Mock private ReportRepo           reportRepo;
//    @Mock private ResolutionRepo resolutionRepository;
//    @Mock private UserRepo             userRepo;
//    @Mock private NotificationService  notificationService;
//
//    private ResolutionServiceImpl service;
//
//    private static final String PHI_EMAIL = "phi@health.gov.lk";
//    private static final Long   REPORT_ID = 42L;
//
//    @BeforeEach
//    void setUp() {
//        service = new ResolutionServiceImpl(reportRepo, resolutionRepository, userRepo, notificationService);
//    }
//
//    private User buildPhi() {
//        User phi = new User();
//        phi.setId(1L);
//        phi.setFname("John");
//        phi.setLname("Doe");
//        phi.setEmail(PHI_EMAIL);
//        return phi;
//    }
//
//    private Report buildReport(ReportStatus status) {
//        Report r = new Report();
//        r.setId(REPORT_ID);
//        r.setDeviceUUID("550e8400-e29b-41d4-a716-446655440000");
//        r.setReportStatus(status);
//        return r;
//    }
//
//    private Resolution buildResolution(Report report, User phi) {
//        Resolution res = new Resolution();
//        res.setId(10L);
//        res.setReport(report);
//        res.setResolvedBy(phi);
//        res.setAction(ResolutionAction.TREATED);
//        res.setNotes("Removed standing water near compound.");
//        res.setResolvedAt(LocalDateTime.now());
//        return res;
//    }
//
//    private ResolutionRequestDTO buildRequest() {
//        ResolutionRequestDTO dto = new ResolutionRequestDTO();
//        dto.setAction(ResolutionAction.TREATED);
//        dto.setNotes("Removed standing water near compound.");
//        return dto;
//    }
//
//    @Nested
//    @DisplayName("resolveReport — happy paths")
//    class ResolveReportHappy {
//
//        @Test
//        @DisplayName("DISPATCHED report is resolved successfully — Resolution saved, Report status set to RESOLVED")
//        void resolveReport_success() {
//            // Arrange
//            Report    report     = buildReport(ReportStatus.DISPATCHED);
//            User      phi        = buildPhi();
//            Resolution resolution = buildResolution(report, phi);
//
//            when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.of(report));
//            when(resolutionRepository.existsByReport_Id(REPORT_ID)).thenReturn(false);
//            when(userRepo.findByEmail(PHI_EMAIL)).thenReturn(phi);
//            when(resolutionRepository.save(any(Resolution.class))).thenReturn(resolution);
//            when(reportRepo.save(any(Report.class))).thenReturn(report);
//
//            // Act
//            ResolutionResponseDTO result = service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL);
//
//            // Assert — response DTO
//            assertThat(result).isNotNull();
//            assertThat(result.getId()).isEqualTo(10L);
//            assertThat(result.getReportId()).isEqualTo(REPORT_ID);
//            assertThat(result.getResolvedByName()).isEqualTo("John Doe");
//            assertThat(result.getAction()).isEqualTo(ResolutionAction.TREATED);
//            assertThat(result.getNotes()).isEqualTo("Removed standing water near compound.");
//
//            // Assert — Report was updated to RESOLVED
//            ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
//            verify(reportRepo).save(reportCaptor.capture());
//            assertThat(reportCaptor.getValue().getReportStatus()).isEqualTo(ReportStatus.RESOLVED);
//            assertThat(reportCaptor.getValue().getResolvedBy()).isEqualTo(phi);
//            assertThat(reportCaptor.getValue().getResolution()).isEqualTo(resolution);
//
//            // Assert — Resolution was saved
//            verify(resolutionRepository).save(any(Resolution.class));
//
//            // Assert — notification was triggered
//            verify(notificationService).notifyResolved(any(Report.class));
//        }
//
//        @Test
//        @DisplayName("Notification failure does not roll back the resolution")
//        void resolveReport_notificationFailureIsSwallowed() {
//            Report    report     = buildReport(ReportStatus.DISPATCHED);
//            User      phi        = buildPhi();
//            Resolution resolution = buildResolution(report, phi);
//
//            when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.of(report));
//            when(resolutionRepository.existsByReport_Id(REPORT_ID)).thenReturn(false);
//            when(userRepo.findByEmail(PHI_EMAIL)).thenReturn(phi);
//            when(resolutionRepository.save(any(Resolution.class))).thenReturn(resolution);
//            when(reportRepo.save(any(Report.class))).thenReturn(report);
//            doThrow(new RuntimeException("FCM unavailable")).when(notificationService).notifyResolved(any());
//
//            // Should NOT throw — notification error is swallowed
//            assertThatNoException().isThrownBy(() ->
//                    service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL));
//
//            // Resolution was still saved
//            verify(resolutionRepository).save(any(Resolution.class));
//        }
//    }
//
//    @Nested
//    @DisplayName("getResolutionByReportId — happy paths")
//    class GetResolutionHappy {
//
//        @Test
//        @DisplayName("Returns resolution DTO for a known report with existing resolution")
//        void getResolutionByReportId_success() {
//            Report    report     = buildReport(ReportStatus.RESOLVED);
//            User      phi        = buildPhi();
//            Resolution resolution = buildResolution(report, phi);
//
//            when(reportRepo.existsById(REPORT_ID)).thenReturn(true);
//            when(resolutionRepository.findByReport_Id(REPORT_ID)).thenReturn(Optional.of(resolution));
//
//            ResolutionResponseDTO result = service.getResolutionByReportId(REPORT_ID);
//
//            assertThat(result.getReportId()).isEqualTo(REPORT_ID);
//            assertThat(result.getResolvedByName()).isEqualTo("John Doe");
//        }
//    }
//
//    @Nested
//    @DisplayName("resolveReport — unhappy paths")
//    class ResolveReportUnhappy {
//
//        @Test
//        @DisplayName("Report not found → NotFoundException (404)")
//        void resolveReport_reportNotFound() {
//            when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining(String.valueOf(REPORT_ID));
//
//            verify(resolutionRepository, never()).save(any());
//        }
//
//        @Test
//        @DisplayName("Already RESOLVED report → InvalidStateException (409)")
//        void resolveReport_alreadyResolved() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.RESOLVED)));
//
//            assertThatThrownBy(() -> service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL))
//                    .isInstanceOf(InvalidStateException.class)
//                    .hasMessageContaining("terminal state")
//                    .hasMessageContaining("RESOLVED");
//        }
//
//        @Test
//        @DisplayName("REJECTED report → InvalidStateException (409)")
//        void resolveReport_alreadyRejected() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.REJECTED)));
//
//            assertThatThrownBy(() -> service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL))
//                    .isInstanceOf(InvalidStateException.class)
//                    .hasMessageContaining("terminal state");
//        }
//
//        @Test
//        @DisplayName("DISMISSED report → InvalidStateException (409)")
//        void resolveReport_alreadyDismissed() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.DISMISSED)));
//
//            assertThatThrownBy(() -> service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL))
//                    .isInstanceOf(InvalidStateException.class)
//                    .hasMessageContaining("terminal state");
//        }
//
//        @Test
//        @DisplayName("CLASSIFIED report (not dispatched yet) → InvalidStateException (409)")
//        void resolveReport_notDispatched_classified() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.CLASSIFIED)));
//
//            assertThatThrownBy(() -> service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL))
//                    .isInstanceOf(InvalidStateException.class)
//                    .hasMessageContaining("DISPATCHED")
//                    .hasMessageContaining("CLASSIFIED");
//        }
//
//        @Test
//        @DisplayName("PENDING report (not dispatched yet) → InvalidStateException (409)")
//        void resolveReport_notDispatched_pending() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.PENDING)));
//
//            assertThatThrownBy(() -> service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL))
//                    .isInstanceOf(InvalidStateException.class)
//                    .hasMessageContaining("DISPATCHED");
//        }
//
//        @Test
//        @DisplayName("Duplicate resolution → DuplicationException (409)")
//        void resolveReport_duplicateResolution() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.DISPATCHED)));
//            when(resolutionRepository.existsByReport_Id(REPORT_ID)).thenReturn(true);
//
//            assertThatThrownBy(() -> service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL))
//                    .isInstanceOf(DuplicationException.class)
//                    .hasMessageContaining("already exists");
//
//            verify(resolutionRepository, never()).save(any());
//        }
//
//        @Test
//        @DisplayName("PHI user not found in DB (stale JWT) → NotFoundException (404)")
//        void resolveReport_phiNotFound() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.DISPATCHED)));
//            when(resolutionRepository.existsByReport_Id(REPORT_ID)).thenReturn(false);
//            when(userRepo.findByEmail(PHI_EMAIL)).thenReturn(null);
//
//            assertThatThrownBy(() -> service.resolveReport(REPORT_ID, buildRequest(), PHI_EMAIL))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining(PHI_EMAIL);
//
//            verify(resolutionRepository, never()).save(any());
//        }
//    }
//
//    @Nested
//    @DisplayName("getResolutionByReportId — unhappy paths")
//    class GetResolutionUnhappy {
//
//        @Test
//        @DisplayName("Report not found → NotFoundException (404)")
//        void getResolution_reportNotFound() {
//            when(reportRepo.existsById(REPORT_ID)).thenReturn(false);
//
//            assertThatThrownBy(() -> service.getResolutionByReportId(REPORT_ID))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining(String.valueOf(REPORT_ID));
//        }
//
//        @Test
//        @DisplayName("Report exists but no resolution yet → NotFoundException (404)")
//        void getResolution_noResolutionYet() {
//            when(reportRepo.existsById(REPORT_ID)).thenReturn(true);
//            when(resolutionRepository.findByReport_Id(REPORT_ID)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> service.getResolutionByReportId(REPORT_ID))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining("No resolution found");
//        }
//    }
//}
