//package com.zeylex.denguesense.service;
//
//import com.zeylex.denguesense.dto.requestDTO.ReportStatusUpdateDTO;
//import com.zeylex.denguesense.dto.responseDTO.ReportResponseDTO;
//import com.zeylex.denguesense.exception.NotFoundException;
//import com.zeylex.denguesense.model.District;
//import com.zeylex.denguesense.model.Report;
//import com.zeylex.denguesense.model.User;
//import com.zeylex.denguesense.model.enums.ReportStatus;
//import com.zeylex.denguesense.repo.ReportRepo;
//import com.zeylex.denguesense.repo.UserRepo;
//import com.zeylex.denguesense.service.impl.ReportServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//@ExtendWith(MockitoExtension.class)
//class ReportServiceImplTest {
//
//    @Mock private ReportRepo              reportRepo;
//    @Mock private UserRepo                userRepo;
//    @Mock private DistrictService         districtService;
//    @Mock private CloudinaryService       cloudinaryService;
//    @Mock private AiClassificationService aiClassificationService;
//
//    private ReportServiceImpl service;
//
//    private static final Long   REPORT_ID    = 7L;
//    private static final String MOH_EMAIL    = "moh@health.gov.lk";
//
//    @BeforeEach
//    void setUp() {
//        service = new ReportServiceImpl(reportRepo, userRepo, districtService,
//                cloudinaryService, aiClassificationService);
//    }
//    private Report buildReport(ReportStatus status) {
//        Report r = new Report();
//        r.setId(REPORT_ID);
//        r.setDeviceUUID("550e8400-e29b-41d4-a716-446655440000");
//        r.setReportStatus(status);
//        District d = new District();
//        d.setName("Colombo");
//        r.setDistrict(d);
//        return r;
//    }
//
//    private User buildUser(String email) {
//        User u = new User();
//        u.setEmail(email);
//        u.setFname("Test");
//        u.setLname("User");
//        return u;
//    }
//
//    private ReportStatusUpdateDTO statusDto(ReportStatus target) {
//        ReportStatusUpdateDTO dto = new ReportStatusUpdateDTO();
//        dto.setStatus(target);
//        return dto;
//    }
//
//    @Nested
//    @DisplayName("updateReportStatus — RESOLVED blocking")
//    class ResolvedBlocking {
//
//        @Test
//        @DisplayName("Setting status to RESOLVED via this endpoint → IllegalArgumentException")
//        void updateStatus_blocksResolved() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.DISPATCHED)));
//
//            assertThatThrownBy(() ->
//                    service.updateReportStatus(REPORT_ID, statusDto(ReportStatus.RESOLVED), MOH_EMAIL))
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("RESOLVED")
//                    .hasMessageContaining("/api/v1/resolutions/");
//        }
//
//        @Test
//        @DisplayName("Setting RESOLVED from PENDING → also blocked by guard before transition check")
//        void updateStatus_blocksResolved_fromPending() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.PENDING)));
//
//            assertThatThrownBy(() ->
//                    service.updateReportStatus(REPORT_ID, statusDto(ReportStatus.RESOLVED), MOH_EMAIL))
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("RESOLVED");
//        }
//    }
//
//    @Nested
//    @DisplayName("updateReportStatus — DISPATCHED stamping")
//    class DispatchStamping {
//
//        @Test
//        @DisplayName("CLASSIFIED → DISPATCHED stamps dispatchedAt and dispatchedBy")
//        void updateStatus_dispatchStampsFields() {
//            Report report = buildReport(ReportStatus.CLASSIFIED);
//            User dispatcher = buildUser(MOH_EMAIL);
//
//            when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.of(report));
//            when(userRepo.findByEmail(MOH_EMAIL)).thenReturn(dispatcher);
//            when(reportRepo.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
//
//            ReportResponseDTO result = service.updateReportStatus(
//                    REPORT_ID, statusDto(ReportStatus.DISPATCHED), MOH_EMAIL);
//
//            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
//            verify(reportRepo).save(captor.capture());
//
//            Report saved = captor.getValue();
//            assertThat(saved.getReportStatus()).isEqualTo(ReportStatus.DISPATCHED);
//            assertThat(saved.getDispatchedAt()).isNotNull();
//            assertThat(saved.getDispatchedBy()).isEqualTo(dispatcher);
//        }
//
//        @Test
//        @DisplayName("CLASSIFIED → DISPATCHED does NOT overwrite existing dispatchedAt (idempotent)")
//        void updateStatus_dispatchIdempotent() {
//            Report report = buildReport(ReportStatus.CLASSIFIED);
//            java.time.LocalDateTime originalTime = java.time.LocalDateTime.of(2026, 1, 1, 12, 0);
//            report.setDispatchedAt(originalTime);
//
//            when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.of(report));
//            when(reportRepo.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
//
//            service.updateReportStatus(REPORT_ID, statusDto(ReportStatus.DISPATCHED), MOH_EMAIL);
//
//            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
//            verify(reportRepo).save(captor.capture());
//            // dispatchedAt must not be changed
//            assertThat(captor.getValue().getDispatchedAt()).isEqualTo(originalTime);
//            // userRepo should not be called since dispatchedBy guard also applies
//            verify(userRepo, never()).findByEmail(any());
//        }
//    }
//    @Nested
//    @DisplayName("updateReportStatus — invalid transitions")
//    class InvalidTransitions {
//
//        @Test
//        @DisplayName("RESOLVED report → any transition → IllegalArgumentException")
//        void updateStatus_fromResolved_blocked() {
//            when(reportRepo.findById(REPORT_ID))
//                    .thenReturn(Optional.of(buildReport(ReportStatus.RESOLVED)));
//
//            assertThatThrownBy(() ->
//                    service.updateReportStatus(REPORT_ID, statusDto(ReportStatus.DISPATCHED), MOH_EMAIL))
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("Invalid status transition");
//        }
//
//        @Test
//        @DisplayName("Report not found → NotFoundException")
//        void updateStatus_reportNotFound() {
//            when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() ->
//                    service.updateReportStatus(REPORT_ID, statusDto(ReportStatus.DISPATCHED), MOH_EMAIL))
//                    .isInstanceOf(NotFoundException.class);
//        }
//    }
//}
