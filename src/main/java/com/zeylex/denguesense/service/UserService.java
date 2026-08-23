package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.requestDTO.RegisterDTO;
import com.zeylex.denguesense.dto.requestDTO.UserUpdateDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import com.zeylex.denguesense.dto.responseDTO.UserResponseDTO;

import java.util.List;

public interface UserService {
    String registerUser(RegisterDTO registerDTO);

    PaginatedDTO getAll(int page, int size);

    PaginatedDTO getUsersByRole(String role, int page, int size);

    PaginatedDTO getUsersByRoles(List<String> roles, int page, int size);

    PaginatedDTO getUsersByRoleAndStatus(String role, String status, int page, int size);

    String updateUserStatus(Long id, String status);

    String updateUser(UserUpdateDTO userUpdateDTO);

    UserResponseDTO getUserById(Long id);

    UserResponseDTO loadUserByUsername(String email);

    String deleteUser(Long id);

    String resetPassword(String email, String newPassword);
}
