package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.responseDTO.DistrictForecastResponseDTO;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.enums.ForecastStatus;
import com.zeylex.denguesense.model.enums.GenerationSource;
import com.zeylex.denguesense.service.ForecastGenerationResult;
import com.zeylex.denguesense.service.ForecastOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the {@code @PreAuthorize} enforcement on {@link DistrictForecastController} through a
 * real method-security proxy (rather than the flaky {@code @WebMvcTest} slice). The regenerate POST
 * is an administrator-only operations override: PHI must be blocked for <em>every</em> district,
 * including one they would otherwise be assigned to. The dashboard read stays open to PHI.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DistrictForecastControllerTest.MethodSecurityConfig.class)
@DisplayName("DistrictForecastController — admin-only regenerate, open dashboard read")
class DistrictForecastControllerTest {

    @EnableMethodSecurity
    @Configuration
    static class MethodSecurityConfig {
        @Bean
        ForecastOrchestrationService orchestrationService() {
            return Mockito.mock(ForecastOrchestrationService.class);
        }

        @Bean
        DistrictForecastController districtForecastController(ForecastOrchestrationService orchestrationService) {
            return new DistrictForecastController(orchestrationService);
        }
    }

    @Autowired private DistrictForecastController controller;
    @Autowired private ForecastOrchestrationService orchestrationService;

    private HttpServletRequest request;

    private static final Integer COLOMBO = 4;
    private static final Integer KALMUNAI = 9;
    private static final LocalDate TARGET = LocalDate.of(2026, 9, 7);

    @BeforeEach
    void setUp() {
        reset(orchestrationService);
        request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/admin/forecasts/4/regenerate");
    }

    private DistrictForecast sampleRow(ForecastStatus status) {
        return DistrictForecast.builder()
                .id(1L).rdhsId(COLOMBO).districtName("Colombo").targetWeekStart(TARGET)
                .predictions(List.of(10.0, 11.0, 12.0, 13.0))
                .lowerBounds(List.of(8.0, 9.0, 10.0, 11.0))
                .upperBounds(List.of(12.0, 13.0, 14.0, 15.0))
                .modelVersion("lstm-v1").status(status)
                .generationSource(GenerationSource.MANUAL)
                .generatedAt(Instant.parse("2026-08-31T02:00:00Z"))
                .build();
    }

    // ── Manual regenerate: administrator-only ─────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can regenerate any district → 200 with the forecast")
    void adminCanRegenerate() {
        when(orchestrationService.generateForecast(eq(COLOMBO), eq(GenerationSource.MANUAL)))
                .thenReturn(ForecastGenerationResult.generated(sampleRow(ForecastStatus.GENERATED)));

        ResponseEntity<Object> response = controller.regenerate(COLOMBO, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(DistrictForecastResponseDTO.class);
        verify(orchestrationService).generateForecast(eq(COLOMBO), eq(GenerationSource.MANUAL));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN regenerate over all 26 rdhs ids is permitted (spot-checked at the boundaries)")
    void adminCanRegenerateAnyDistrict() {
        when(orchestrationService.generateForecast(any(), eq(GenerationSource.MANUAL)))
                .thenReturn(ForecastGenerationResult.generated(sampleRow(ForecastStatus.GENERATED)));

        for (int rdhsId = 0; rdhsId <= 25; rdhsId++) {
            ResponseEntity<Object> response = controller.regenerate(rdhsId, request);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN regenerate with incomplete history → 422")
    void adminIncompleteHistory() {
        when(orchestrationService.generateForecast(eq(COLOMBO), eq(GenerationSource.MANUAL)))
                .thenReturn(ForecastGenerationResult.incompleteHistory(null, "Only 6 of 8 weeks available"));

        ResponseEntity<Object> response = controller.regenerate(COLOMBO, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @WithMockUser(roles = "PHI")
    @DisplayName("PHI is blocked from regenerate for a district that is NOT theirs → 403, service untouched")
    void phiForbiddenOtherDistrict() {
        assertThatThrownBy(() -> controller.regenerate(KALMUNAI, request))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(orchestrationService, never()).generateForecast(any(), any(GenerationSource.class));
    }

    @Test
    @WithMockUser(roles = "PHI")
    @DisplayName("PHI is blocked from regenerate even for their OWN district → 403 (no scoping bypass)")
    void phiForbiddenOwnDistrict() {
        assertThatThrownBy(() -> controller.regenerate(COLOMBO, request))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(orchestrationService, never()).generateForecast(any(), any(GenerationSource.class));
    }

    @Test
    @WithMockUser(roles = {"MOH", "EPIDEMIOLOGIST"})
    @DisplayName("Other non-admin roles are blocked from regenerate → 403")
    void otherRolesForbidden() {
        assertThatThrownBy(() -> controller.regenerate(COLOMBO, request))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(orchestrationService, never()).generateForecast(any(), any(GenerationSource.class));
    }

    // ── Dashboard read: open to dashboard-facing roles ────────────────────────

    @Test
    @WithMockUser(roles = "PHI")
    @DisplayName("PHI can read the latest forecast → 200 with status + generatedAt populated")
    void phiCanReadLatest() {
        when(orchestrationService.getLatestForecast(COLOMBO)).thenReturn(sampleRow(ForecastStatus.STALE));

        ResponseEntity<DistrictForecastResponseDTO> response = controller.latest(COLOMBO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(ForecastStatus.STALE);
        assertThat(response.getBody().generatedAt()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "PHI")
    @DisplayName("Reading a district with no forecast yet → NotFoundException (404)")
    void readLatestNotFound() {
        when(orchestrationService.getLatestForecast(COLOMBO))
                .thenThrow(new NotFoundException("No forecast generated yet for rdhsId=" + COLOMBO));

        assertThatThrownBy(() -> controller.latest(COLOMBO))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous caller is blocked from regenerate")
    void anonymousForbidden() {
        assertThatThrownBy(() -> controller.regenerate(COLOMBO, request))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(orchestrationService, never()).generateForecast(any(), any(GenerationSource.class));
    }
}
