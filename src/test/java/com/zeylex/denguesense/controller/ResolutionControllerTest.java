//package com.zeylex.denguesense.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.zeylex.denguesense.dto.requestDTO.ResolutionRequestDTO;
//import com.zeylex.denguesense.dto.responseDTO.ResolutionResponseDTO;
//import com.zeylex.denguesense.exception.DuplicationException;
//import com.zeylex.denguesense.exception.GlobalExceptionHandler;
//import com.zeylex.denguesense.exception.InvalidStateException;
//import com.zeylex.denguesense.exception.NotFoundException;
//import com.zeylex.denguesense.model.enums.ResolutionAction;
//import com.zeylex.denguesense.service.ResolutionService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDateTime;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(ResolutionController.class)
//@Import(GlobalExceptionHandler.class)
//class ResolutionControllerTest {
//
//    @Autowired private MockMvc       mockMvc;
//    @Autowired private ObjectMapper  objectMapper;
//    @MockitoBean private ResolutionService resolutionService;
//
//    private static final Long   REPORT_ID  = 42L;
//    private static final String PHI_EMAIL  = "phi@health.gov.lk";
//    private static final String BASE_URL   = "/api/v1/resolutions/";
//
//    private ResolutionRequestDTO buildRequest() {
//        ResolutionRequestDTO dto = new ResolutionRequestDTO();
//        dto.setAction(ResolutionAction.TREATED);
//        dto.setNotes("Removed standing water near compound.");
//        return dto;
//    }
//
//    private ResolutionResponseDTO buildResponse() {
//        return ResolutionResponseDTO.builder()
//                .id(10L)
//                .reportId(REPORT_ID)
//                .resolvedByName("John Doe")
//                .resolvedAt(LocalDateTime.of(2026, 8, 26, 9, 0, 0))
//                .action(ResolutionAction.TREATED)
//                .notes("Removed standing water near compound.")
//                .build();
//    }
//
//    @Nested
//    @DisplayName("POST /api/v1/resolutions/{reportId}")
//    class PostResolve {
//
//        @Test
//        @WithMockUser(username = PHI_EMAIL, roles = "PHI")
//        @DisplayName("PHI can resolve a DISPATCHED report → 201 Created")
//        void resolveReport_phi_success() throws Exception {
//            when(resolutionService.resolveReport(eq(REPORT_ID), any(), eq(PHI_EMAIL)))
//                    .thenReturn(buildResponse());
//
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(buildRequest())))
//                    .andExpect(status().isCreated())
//                    .andExpect(jsonPath("$.id").value(10))
//                    .andExpect(jsonPath("$.reportId").value(REPORT_ID))
//                    .andExpect(jsonPath("$.resolvedByName").value("John Doe"))
//                    .andExpect(jsonPath("$.action").value("TREATED"))
//                    .andExpect(jsonPath("$.notes").value("Removed standing water near compound."));
//
//            verify(resolutionService).resolveReport(eq(REPORT_ID), any(), eq(PHI_EMAIL));
//        }
//
//        @Test
//        @WithMockUser(roles = "MOH")
//        @DisplayName("MOH can also resolve a report → 201 Created")
//        void resolveReport_moh_success() throws Exception {
//            when(resolutionService.resolveReport(any(), any(), any()))
//                    .thenReturn(buildResponse());
//
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(buildRequest())))
//                    .andExpect(status().isCreated());
//        }
//
//        @Test
//        @WithMockUser(roles = "EPIDEMIOLOGIST")
//        @DisplayName("EPIDEMIOLOGIST cannot resolve → 403 Forbidden")
//        void resolveReport_epidemiologist_forbidden() throws Exception {
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(buildRequest())))
//                    .andExpect(status().isForbidden());
//
//            verify(resolutionService, never()).resolveReport(any(), any(), any());
//        }
//
//        @Test
//        @DisplayName("Unauthenticated request → 401 Unauthorized")
//        void resolveReport_unauthenticated() throws Exception {
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(buildRequest())))
//                    .andExpect(status().isUnauthorized());
//        }
//
//        @Test
//        @WithMockUser(roles = "PHI")
//        @DisplayName("Missing 'action' field → 400 Bad Request (validation)")
//        void resolveReport_missingAction() throws Exception {
//            ResolutionRequestDTO invalid = new ResolutionRequestDTO();
//            invalid.setNotes("Some notes");
//            // action is null → @NotNull fails
//
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalid)))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @WithMockUser(roles = "PHI")
//        @DisplayName("Blank 'notes' field → 400 Bad Request (validation)")
//        void resolveReport_blankNotes() throws Exception {
//            ResolutionRequestDTO invalid = new ResolutionRequestDTO();
//            invalid.setAction(ResolutionAction.TREATED);
//            invalid.setNotes("   ");  // blank
//
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalid)))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @WithMockUser(roles = "PHI")
//        @DisplayName("Report not found → 404 Not Found")
//        void resolveReport_notFound() throws Exception {
//            when(resolutionService.resolveReport(eq(REPORT_ID), any(), any()))
//                    .thenThrow(new NotFoundException("Report not found with id: " + REPORT_ID));
//
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(buildRequest())))
//                    .andExpect(status().isNotFound())
//                    .andExpect(jsonPath("$.message").value("Report not found with id: " + REPORT_ID));
//        }
//
//        @Test
//        @WithMockUser(roles = "PHI")
//        @DisplayName("Report already RESOLVED → 409 Conflict")
//        void resolveReport_alreadyResolved() throws Exception {
//            when(resolutionService.resolveReport(eq(REPORT_ID), any(), any()))
//                    .thenThrow(new InvalidStateException(
//                            "Report id=42 is already in a terminal state: RESOLVED."));
//
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(buildRequest())))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.status").value(409));
//        }
//
//        @Test
//        @WithMockUser(roles = "PHI")
//        @DisplayName("Duplicate resolution → 409 Conflict")
//        void resolveReport_duplicate() throws Exception {
//            when(resolutionService.resolveReport(eq(REPORT_ID), any(), any()))
//                    .thenThrow(new DuplicationException(
//                            "A resolution already exists for report id=" + REPORT_ID));
//
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(buildRequest())))
//                    .andExpect(status().isConflict());
//        }
//
//        @Test
//        @WithMockUser(roles = "PHI")
//        @DisplayName("Report not in DISPATCHED state (e.g. CLASSIFIED) → 409 Conflict")
//        void resolveReport_notDispatched() throws Exception {
//            when(resolutionService.resolveReport(eq(REPORT_ID), any(), any()))
//                    .thenThrow(new InvalidStateException(
//                            "Report id=42 must be in DISPATCHED state. Current: CLASSIFIED"));
//
//            mockMvc.perform(post(BASE_URL + REPORT_ID)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(buildRequest())))
//                    .andExpect(status().isConflict());
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /api/v1/resolutions/{reportId}")
//    class GetResolution {
//
//        @Test
//        @WithMockUser(roles = "PHI")
//        @DisplayName("PHI can retrieve a resolution → 200 OK")
//        void getResolution_phi_success() throws Exception {
//            when(resolutionService.getResolutionByReportId(REPORT_ID))
//                    .thenReturn(buildResponse());
//
//            mockMvc.perform(get(BASE_URL + REPORT_ID))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.reportId").value(REPORT_ID))
//                    .andExpect(jsonPath("$.action").value("TREATED"));
//        }
//
//        @Test
//        @WithMockUser(roles = "EPIDEMIOLOGIST")
//        @DisplayName("EPIDEMIOLOGIST can read resolution → 200 OK")
//        void getResolution_epidemiologist_success() throws Exception {
//            when(resolutionService.getResolutionByReportId(REPORT_ID))
//                    .thenReturn(buildResponse());
//
//            mockMvc.perform(get(BASE_URL + REPORT_ID))
//                    .andExpect(status().isOk());
//        }
//
//        @Test
//        @WithMockUser(roles = "PHI")
//        @DisplayName("No resolution yet → 404 Not Found")
//        void getResolution_notFound() throws Exception {
//            when(resolutionService.getResolutionByReportId(REPORT_ID))
//                    .thenThrow(new NotFoundException("No resolution found for report id: " + REPORT_ID));
//
//            mockMvc.perform(get(BASE_URL + REPORT_ID))
//                    .andExpect(status().isNotFound())
//                    .andExpect(jsonPath("$.message").value("No resolution found for report id: " + REPORT_ID));
//        }
//    }
//}
