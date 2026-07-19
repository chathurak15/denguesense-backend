package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.requestDTO.UserUpdateDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import com.zeylex.denguesense.dto.responseDTO.UserResponseDTO;
import com.zeylex.denguesense.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/user")
@CrossOrigin
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Get All
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MOH')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (size > 50) {
            return ResponseEntity.badRequest().body("Item size is too large! Maximum allowed is 50.");
        }
        return ResponseEntity.ok(userService.getAll(page, size));
    }

    // Get By Role
    @GetMapping("/by-role")
    @PreAuthorize("hasAnyRole('ADMIN', 'MOH')")
    public ResponseEntity<?> getUsersByRole(
            @RequestParam String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 50) return ResponseEntity.badRequest().body("Size too large");
        try {
            PaginatedDTO result = userService.getUsersByRole(role, page, size);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role: " + role));
        }
    }

    // Get Writers + Editors combined
    @GetMapping("/phi")
    @PreAuthorize("hasAnyRole('ADMIN', 'MOH')")
    public ResponseEntity<?> getPHIUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 50) return ResponseEntity.badRequest().body("Size too large");
        PaginatedDTO result = userService.getUsersByRoles(List.of("PHI"), page, size);
        return ResponseEntity.ok(result);
    }

    // Get By Role + Status
    @GetMapping("/by-role-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MOH')")
    public ResponseEntity<?> getUsersByRoleAndStatus(
            @RequestParam String role,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 50) return ResponseEntity.badRequest().body("Size too large");
        try {
            PaginatedDTO result = userService.getUsersByRoleAndStatus(role, status, page, size);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role or status"));
        }
    }


    // Update Status
    @PutMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','MOH')")
    public ResponseEntity<String> updateUserStatus(
            @RequestParam Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(userService.updateUserStatus(id, status));
    }


    // Update Profile
    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHI', 'MOH', 'VOL', 'EPIDEMIOLOGIST','VOLUNTEER')")
    public ResponseEntity<String> updateUser(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        return ResponseEntity.ok(userService.updateUser(userUpdateDTO));
    }

    // Get By ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'CHIEF_EDITOR')")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id) {
        UserResponseDTO user = userService.getUserById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    // Delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deleteUser(id));
    }

    // Current User
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER', 'WRITER', 'EDITOR', 'CHIEF_EDITOR')")
    public ResponseEntity<UserResponseDTO> getCurrentUserInfo(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        return ResponseEntity.ok(userService.loadUserByUsername(userDetails.getUsername()));
    }
}
