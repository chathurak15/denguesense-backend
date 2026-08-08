//package com.zeylex.denguesense.controller;
//
//import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
//import com.zeylex.denguesense.dto.responseDTO.ResolutionResponseDTO;
//import com.zeylex.denguesense.exception.GlobalExceptionHandler;
//import com.zeylex.denguesense.exception.NotFoundException;
//import com.zeylex.denguesense.model.enums.ResolutionAction;
//import com.zeylex.denguesense.service.ReportService;
//import com.zeylex.denguesense.service.ResolutionService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(ReportController.class)
//@Import(GlobalExceptionHandler.class)
//class PhiReportControllerTest {
//
//    @Autowired  private MockMvc          mockMvc;
//    @MockitoBean private ReportService   reportService;
//    @MockitoBean private ResolutionService resolutionService;
//
//    private static final String PHI_EMAIL  = "phi@health.gov.lk";
//    private static final Long   REPORT_ID  = 42L;
//
//    private PaginatedDTO emptyPage() {
//        return new PaginatedDTO(Collections.emptyList(), 0, 0L);
//    }
//
//    private ResolutionResponseDTO buildResolution() {
//        return ResolutionResponseDTO.builder()
//                .id(1L)
//                .reportId(REPORT_ID)
//                .resolvedByName("John Doe")
//                .resolvedAt(LocalDateTime.of(2026, 8, 26, 10, 0))
//                .action(ResolutionAction.TREATED)
//                .notes("Area treated successfully.")
//                .build();
//    }
//
//    // ── GET /phi/district ─────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("GET /api/v1/reports/phi/district")
//    class GetDistrictReports {
//
//        @Test
//        @WithMockUser(username = PHI_EMAIL, roles = "PHI")
//        @DisplayName("PHI with assigned district → 200 OK")
//        void phiCanGetDistrictReports() throws Exception {
//            when(reportService.getDistrictReports(eq(PHI_EMAIL), any())).thenReturn(emptyPage());
//
//            mockMvc.perform(get("/api/v1/reports/phi/district"))
//                    .andExpect(status().isOk());
//
//            verify(reportService).getDistrictReports(eq(PHI_EMAIL), any());
//        }
//
//        @Test
//        @WithMockUser(roles = "MOH")
//        @DisplayName("MOH cannot access PHI district endpoint → 403 Forbidden")
//        void mohForbidden() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/district"))
//                    .andExpect(status().isForbidden());
//
//            verify(reportService, never()).getDistrictReports(any(), any());
//        }
//
//        @Test
//        @WithMockUser(roles = "EPIDEMIOLOGIST")
//        @DisplayName("EPIDEMIOLOGIST cannot access PHI district endpoint → 403 Forbidden")
//        void epidemiologistForbidden() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/district"))
//                    .andExpect(status().isForbidden());
//        }
//
//        @Test
//        @WithMockUser(roles = "ADMIN")
//        @DisplayName("ADMIN cannot access PHI district endpoint → 403 Forbidden")
//        void adminForbidden() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/district"))
//                    .andExpect(status().isForbidden());
//        }
//
//        @Test
//        @DisplayName("Unauthenticated → 401 Unauthorized")
//        void unauthenticated() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/district"))
//                    .andExpect(status().isUnauthorized());
//        }
//
//        @Test
//        @WithMockUser(username = PHI_EMAIL, roles = "PHI")
//        @DisplayName("PHI with no district assigned → 404 Not Found")
//        void phiWithNoDistrictReturns404() throws Exception {
//            when(reportService.getDistrictReports(eq(PHI_EMAIL), any()))
//                    .thenThrow(new NotFoundException("PHI user '" + PHI_EMAIL + "' has no district assigned."));
//
//            mockMvc.perform(get("/api/v1/reports/phi/district"))
//                    .andExpect(status().isNotFound())
//                    .andExpect(jsonPath("$.message").value(
//                            "PHI user '" + PHI_EMAIL + "' has no district assigned."));
//        }
//    }
//
//    // ── GET /phi/district/resolved ────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("GET /api/v1/reports/phi/district/resolved")
//    class GetDistrictResolvedReports {
//
//        @Test
//        @WithMockUser(username = PHI_EMAIL, roles = "PHI")
//        @DisplayName("PHI → 200 OK with district resolved reports")
//        void phiCanGetDistrictResolved() throws Exception {
//            when(reportService.getDistrictResolvedReports(eq(PHI_EMAIL), any())).thenReturn(emptyPage());
//
//            mockMvc.perform(get("/api/v1/reports/phi/district/resolved"))
//                    .andExpect(status().isOk());
//
//            verify(reportService).getDistrictResolvedReports(eq(PHI_EMAIL), any());
//        }
//
//        @Test
//        @WithMockUser(roles = "MOH")
//        @DisplayName("MOH → 403 Forbidden")
//        void mohForbidden() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/district/resolved"))
//                    .andExpect(status().isForbidden());
//        }
//
//        @Test
//        @DisplayName("Unauthenticated → 401")
//        void unauthenticated() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/district/resolved"))
//                    .andExpect(status().isUnauthorized());
//        }
//    }
//
//    // ── GET /phi/my-resolved ──────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("GET /api/v1/reports/phi/my-resolved")
//    class GetMyResolved {
//
//        @Test
//        @WithMockUser(username = PHI_EMAIL, roles = "PHI")
//        @DisplayName("PHI → 200 OK with own resolved reports")
//        void phiCanGetMyResolved() throws Exception {
//            when(reportService.getMyResolvedReports(eq(PHI_EMAIL), any())).thenReturn(emptyPage());
//
//            mockMvc.perform(get("/api/v1/reports/phi/my-resolved"))
//                    .andExpect(status().isOk());
//
//            verify(reportService).getMyResolvedReports(eq(PHI_EMAIL), any());
//        }
//
//        @Test
//        @WithMockUser(roles = "EPIDEMIOLOGIST")
//        @DisplayName("EPIDEMIOLOGIST → 403 Forbidden")
//        void epidemiologistForbidden() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/my-resolved"))
//                    .andExpect(status().isForbidden());
//        }
//
//        @Test
//        @DisplayName("Unauthenticated → 401")
//        void unauthenticated() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/my-resolved"))
//                    .andExpect(status().isUnauthorized());
//        }
//    }
//
//    // ── GET /phi/district/resolved/{reportId} ─────────────────────────────────
//
//    @Nested
//    @DisplayName("GET /api/v1/reports/phi/district/resolved/{reportId}")
//    class GetDistrictResolutionDetail {
//
//        @Test
//        @WithMockUser(username = PHI_EMAIL, roles = "PHI")
//        @DisplayName("PHI can fetch resolution detail for a report in their district → 200 OK")
//        void phiCanGetResolutionDetail() throws Exception {
//            when(resolutionService.getDistrictResolutionByReportId(eq(REPORT_ID), eq(PHI_EMAIL)))
//                    .thenReturn(buildResolution());
//
//            mockMvc.perform(get("/api/v1/reports/phi/district/resolved/" + REPORT_ID))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.reportId").value(REPORT_ID))
//                    .andExpect(jsonPath("$.resolvedByName").value("John Doe"))
//                    .andExpect(jsonPath("$.action").value("TREATED"))
//                    .andExpect(jsonPath("$.notes").value("Area treated successfully."));
//
//            verify(resolutionService).getDistrictResolutionByReportId(eq(REPORT_ID), eq(PHI_EMAIL));
//        }
//
//        @Test
//        @WithMockUser(username = PHI_EMAIL, roles = "PHI")
//        @DisplayName("Report not in PHI's district → 404 Not Found")
//        void reportOutsideDistrictReturns404() throws Exception {
//            when(resolutionService.getDistrictResolutionByReportId(eq(REPORT_ID), eq(PHI_EMAIL)))
//                    .thenThrow(new NotFoundException(
//                            "No resolution found for report id: " + REPORT_ID + " in district id: 1"));
//
//            mockMvc.perform(get("/api/v1/reports/phi/district/resolved/" + REPORT_ID))
//                    .andExpect(status().isNotFound())
//                    .andExpect(jsonPath("$.message").value(
//                            "No resolution found for report id: " + REPORT_ID + " in district id: 1"));
//        }
//
//        @Test
//        @WithMockUser(roles = "MOH")
//        @DisplayName("MOH → 403 Forbidden")
//        void mohForbidden() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/district/resolved/" + REPORT_ID))
//                    .andExpect(status().isForbidden());
//        }
//
//        @Test
//        @DisplayName("Unauthenticated → 401")
//        void unauthenticated() throws Exception {
//            mockMvc.perform(get("/api/v1/reports/phi/district/resolved/" + REPORT_ID))
//                    .andExpect(status().isUnauthorized());
//        }
//
//        @Test
//        @WithMockUser(username = PHI_EMAIL, roles = "PHI")
//        @DisplayName("Response must NOT contain resolvedByEmail field (privacy guard)")
//        void responseDoesNotExposeEmail() throws Exception {
//            when(resolutionService.getDistrictResolutionByReportId(eq(REPORT_ID), eq(PHI_EMAIL)))
//                    .thenReturn(buildResolution());
//
//            mockMvc.perform(get("/api/v1/reports/phi/district/resolved/" + REPORT_ID))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.resolvedByEmail").doesNotExist());
//        }
//    }
//}
