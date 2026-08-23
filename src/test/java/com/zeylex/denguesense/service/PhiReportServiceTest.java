//package com.zeylex.denguesense.service;
//
//import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
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
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class PhiReportServiceTest {
//
//    @Mock private ReportRepo reportRepo;
//    @Mock private UserRepo userRepo;
//    @Mock private DistrictService districtService;
//    @Mock private CloudinaryService cloudinaryService;
//    @Mock private AiClassificationService aiClassificationService;
//
//    private ReportServiceImpl service;
//
//    private static final String PHI_EMAIL = "phi@health.gov.lk";
//    private static final Long DISTRICT_ID = 1L;
//    private static final Long PHI_ID = 100L;
//
//    @BeforeEach
//    void setUp() {
//        service = new ReportServiceImpl(reportRepo, userRepo, districtService,
//                cloudinaryService, aiClassificationService);
//    }
//
//    private User buildPhiWithDistrict() {
//        User phi = new User();
//        phi.setId(PHI_ID);
//        phi.setEmail(PHI_EMAIL);
//        District district = new District();
//        district.setId(DISTRICT_ID);
//        phi.setDistrict(district);
//        return phi;
//    }
//
//    private User buildPhiWithoutDistrict() {
//        User phi = new User();
//        phi.setId(PHI_ID);
//        phi.setEmail(PHI_EMAIL);
//        phi.setDistrict(null);
//        return phi;
//    }
//
//    private Page<Report> emptyPage() {
//        return new PageImpl<>(List.of());
//    }
//
//    @Nested
//    @DisplayName("getDistrictReports")
//    class GetDistrictReports {
//        @Test
//        @DisplayName("Returns reports for PHI's district")
//        void success() {
//            when(userRepo.findByEmail(PHI_EMAIL)).thenReturn(buildPhiWithDistrict());
//            when(reportRepo.findByDistrict_Id(eq(DISTRICT_ID), any(Pageable.class))).thenReturn(emptyPage());
//
//            PaginatedDTO result = service.getDistrictReports(PHI_EMAIL, PageRequest.of(0, 10));
//
//            assertThat(result).isNotNull();
//            assertThat(result.getContent()).isEmpty();
//        }
//
//        @Test
//        @DisplayName("Throws NotFoundException if PHI user not found")
//        void phiNotFound() {
//            when(userRepo.findByEmail(PHI_EMAIL)).thenReturn(null);
//
//            assertThatThrownBy(() -> service.getDistrictReports(PHI_EMAIL, PageRequest.of(0, 10)))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining("Authenticated user not found");
//        }
//
//        @Test
//        @DisplayName("Throws NotFoundException if PHI has no district assigned")
//        void phiHasNoDistrict() {
//            when(userRepo.findByEmail(PHI_EMAIL)).thenReturn(buildPhiWithoutDistrict());
//
//            assertThatThrownBy(() -> service.getDistrictReports(PHI_EMAIL, PageRequest.of(0, 10)))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining("no district assigned");
//        }
//    }
//
//    @Nested
//    @DisplayName("getDistrictResolvedReports")
//    class GetDistrictResolvedReports {
//        @Test
//        @DisplayName("Returns resolved reports for PHI's district")
//        void success() {
//            when(userRepo.findByEmail(PHI_EMAIL)).thenReturn(buildPhiWithDistrict());
//            when(reportRepo.findByDistrict_IdAndReportStatus(eq(DISTRICT_ID), eq(ReportStatus.RESOLVED), any(Pageable.class))).thenReturn(emptyPage());
//
//            PaginatedDTO result = service.getDistrictResolvedReports(PHI_EMAIL, PageRequest.of(0, 10));
//
//            assertThat(result).isNotNull();
//            assertThat(result.getContent()).isEmpty();
//        }
//    }
//
//    @Nested
//    @DisplayName("getMyResolvedReports")
//    class GetMyResolvedReports {
//        @Test
//        @DisplayName("Returns resolved reports for specific PHI")
//        void success() {
//            when(userRepo.findByEmail(PHI_EMAIL)).thenReturn(buildPhiWithDistrict());
//            when(reportRepo.findByResolvedBy_IdAndReportStatus(eq(PHI_ID), eq(ReportStatus.RESOLVED), any(Pageable.class))).thenReturn(emptyPage());
//
//            PaginatedDTO result = service.getMyResolvedReports(PHI_EMAIL, PageRequest.of(0, 10));
//
//            assertThat(result).isNotNull();
//            assertThat(result.getContent()).isEmpty();
//        }
//    }
//}
