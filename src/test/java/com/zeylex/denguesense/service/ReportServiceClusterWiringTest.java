package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.ai.ClassifyResponseDTO;
import com.zeylex.denguesense.dto.requestDTO.ReportSubmitDTO;
import com.zeylex.denguesense.dto.responseDTO.ReportResponseDTO;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.enums.LandType;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.repo.ReportRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Report submission ↔ cluster detection wiring (failure isolation)")
class ReportServiceClusterWiringTest {

    @Mock private ReportRepo reportRepo;
    @Mock private UserRepo userRepo;
    @Mock private DistrictService districtService;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private AiClassificationService aiClassificationService;
    @Mock private ClusterDetectionTrigger clusterDetectionTrigger;
    @Mock private MultipartFile image;

    private ReportServiceImpl service;

    private static final String DEVICE_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(reportRepo, userRepo, districtService,
                cloudinaryService, aiClassificationService, clusterDetectionTrigger);
    }

    private ReportSubmitDTO submitDto() {
        ReportSubmitDTO dto = new ReportSubmitDTO();
        dto.setLatitude(6.9271);
        dto.setLongitude(79.8612);
        dto.setLandType(LandType.PUBLIC);
        return dto;
    }

    private District colombo() {
        District d = new District();
        d.setId(1L);
        d.setName("Colombo");
        return d;
    }

    @Test
    @DisplayName("Clustering exception does NOT fail the citizen's report submission")
    void clusteringFailure_reportStillPersists() {
        when(cloudinaryService.uploadReportImage(any(), anyString())).thenReturn("https://img/x.jpg");
        when(districtService.findByCoordinates(anyDouble(), anyDouble())).thenReturn(colombo());
        when(reportRepo.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(13L);
            }
            return r;
        });
        when(aiClassificationService.classify(anyString()))
                .thenReturn(new ClassifyResponseDTO("HIGH_RISK", 0.97, "cnn-v1"));
        // Detection blows up — must be swallowed
        doThrow(new RuntimeException("boom")).when(clusterDetectionTrigger).triggerForClassifiedReport(anyLong());

        assertThatCode(() -> service.saveReport(DEVICE_UUID, submitDto(), image))
                .doesNotThrowAnyException();

        // Report was persisted and reached CLASSIFIED; detection was attempted
        verify(reportRepo, atLeastOnce()).save(any(Report.class));
        verify(clusterDetectionTrigger).triggerForClassifiedReport(13L);
    }

    @Test
    @DisplayName("HIGH_RISK classified report triggers inline cluster detection")
    void highRiskClassified_triggersDetection() {
        when(cloudinaryService.uploadReportImage(any(), anyString())).thenReturn("https://img/x.jpg");
        when(districtService.findByCoordinates(anyDouble(), anyDouble())).thenReturn(colombo());
        when(reportRepo.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(21L);
            }
            return r;
        });
        when(aiClassificationService.classify(anyString()))
                .thenReturn(new ClassifyResponseDTO("HIGH_RISK", 0.9, "cnn-v1"));

        service.saveReport(DEVICE_UUID, submitDto(), image);

        verify(clusterDetectionTrigger, atLeastOnce()).triggerForClassifiedReport(anyLong());
    }

    @Test
    @DisplayName("Rejected (INVALID) report does NOT trigger cluster detection")
    void invalidReport_noDetection() {
        when(cloudinaryService.uploadReportImage(any(), anyString())).thenReturn("https://img/x.jpg");
        when(districtService.findByCoordinates(anyDouble(), anyDouble())).thenReturn(colombo());
        when(reportRepo.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(30L);
            }
            return r;
        });
        when(aiClassificationService.classify(anyString()))
                .thenReturn(new ClassifyResponseDTO("INVALID", 0.5, "cnn-v1"));

        service.saveReport(DEVICE_UUID, submitDto(), image);

        // INVALID → REJECTED, not CLASSIFIED, so no clustering
        verify(clusterDetectionTrigger, org.mockito.Mockito.never()).triggerForClassifiedReport(anyLong());
    }

    @Test
    @DisplayName("Report still commits with status CLASSIFIED even when detection is invoked")
    void reportStatusIsClassified() {
        when(cloudinaryService.uploadReportImage(any(), anyString())).thenReturn("https://img/x.jpg");
        when(districtService.findByCoordinates(anyDouble(), anyDouble())).thenReturn(colombo());
        when(reportRepo.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(41L);
            }
            return r;
        });
        when(aiClassificationService.classify(anyString()))
                .thenReturn(new ClassifyResponseDTO("HIGH_RISK", 0.88, "cnn-v1"));

        ReportResponseDTO dto = service.saveReport(DEVICE_UUID, submitDto(), image);

        org.assertj.core.api.Assertions.assertThat(dto.getReportStatus()).isEqualTo(ReportStatus.CLASSIFIED);
    }
}
