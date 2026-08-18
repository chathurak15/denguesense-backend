package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.requestDTO.RegisterDTO;
import com.zeylex.denguesense.dto.requestDTO.UserUpdateDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import com.zeylex.denguesense.dto.responseDTO.UserResponseDTO;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.RoleType;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DistrictRepo districtRepo;

    @Override
    public String registerUser(RegisterDTO registerDTO) {
        RoleType requestedRole = registerDTO.getRole();
        if (requestedRole == RoleType.ADMIN || requestedRole == RoleType.MOH) {
            return "Try again! Incorrect user role";
        }
        if (userRepo.existsByEmail(registerDTO.getEmail())) {
            return "Email already in use";
        }
        User user = modelMapper.map(registerDTO, User.class);
        user.setId(null);
        user.setDistrict(districtRepo.findById((long) registerDTO.getDistrictId()).orElse(null));
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setStatus(requestedRole == RoleType.VOLUNTEER ? "APPROVED" : "PENDING");
        try {
            userRepo.save(user);
            return "User registered successfully";
        } catch (Exception e) {
            return "Registration failed: " + e.getMessage();
        }
    }

    @Override
    public PaginatedDTO getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> users = userRepo.findAll(pageable);
        return mapPageToDto(users);
    }

    @Override
    public PaginatedDTO getUsersByRole(String role, int page, int size) {
        RoleType roleType = RoleType.valueOf(role.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> users = userRepo.findByRole(roleType, pageable);
        return mapPageToDto(users);
    }

    @Override
    public PaginatedDTO getUsersByRoles(List<String> roles, int page, int size) {
        List<RoleType> roleTypes = roles.stream()
                .map(r -> RoleType.valueOf(r.toUpperCase()))
                .collect(Collectors.toList());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> users = userRepo.findByRoleIn(roleTypes, pageable);
        return mapPageToDto(users);
    }

    @Override
    public PaginatedDTO getUsersByRoleAndStatus(String role, String status, int page, int size) {
        RoleType roleType = RoleType.valueOf(role.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> users = userRepo.findByRoleAndStatus(roleType, status.toUpperCase(), pageable);
        return mapPageToDto(users);
    }

    @Override
    public String updateUserStatus(Long id, String status) {
        List<String> validStatuses = List.of("APPROVED", "PENDING", "REJECTED", "UNAVAILABLE");
        String upperStatus = status.toUpperCase();
        if (!validStatuses.contains(upperStatus)) {
            return "Invalid status";
        }
        User user = userRepo.findById(id).orElse(null);
        if (user == null) return "User not found";

        user.setStatus(upperStatus);
        userRepo.save(user);
        return "User status updated successfully";
    }

    @Override
    public String updateUser(UserUpdateDTO userUpdateDTO) {
        User user = userRepo.findByEmail(userUpdateDTO.getEmail());
        if (user == null) {
            return "User with email " + userUpdateDTO.getEmail() + " not found";
        }
        user.setFname(userUpdateDTO.getFname());
        user.setLname(userUpdateDTO.getLname());
        user.setPhoneNumber(userUpdateDTO.getPhoneNumber());
        user.setDistrict(districtRepo.findById((long) userUpdateDTO.getDistrictId()).orElse(null));
        user.setImage(userUpdateDTO.getImage());
        userRepo.save(user);
        return "User updated successfully";
    }

    @Override
    public UserResponseDTO getUserById(Long id) {
        return userRepo.findById(id)
                .map(u -> modelMapper.map(u, UserResponseDTO.class))
                .orElse(null);
    }

    @Override
    public UserResponseDTO loadUserByUsername(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) throw new NotFoundException("User not found");
        UserResponseDTO dto = modelMapper.map(user, UserResponseDTO.class);
        return dto;
    }

    @Override
    public String deleteUser(Long id) {
        if (!userRepo.existsById(id) || id == 1) {
            return "User not found";
        }
        userRepo.deleteById(id);
        return "User deleted successfully";
    }

    @Override
    public String resetPassword(String email, String newPassword) {
        User user = userRepo.findByEmail(email);
        if (user == null) return "User not found";
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        return "Password reset successfully";
    }

    private PaginatedDTO mapPageToDto(Page<User> page) {
        if (!page.hasContent()) {
            PaginatedDTO empty = new PaginatedDTO();
            empty.setContent(List.of());
            empty.setTotalItems(0);
            empty.setTotalPages(0);
            return empty;
        }
        List<UserResponseDTO> dtos = page.getContent().stream()
                .map(u -> modelMapper.map(u, UserResponseDTO.class))
                .collect(Collectors.toList());
        PaginatedDTO result = new PaginatedDTO();
        result.setContent(dtos);
        result.setTotalItems(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        return result;
    }
}
