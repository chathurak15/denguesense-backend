package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    User findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole(RoleType roleType, Pageable pageable);

    Page<User> findByRoleIn(List<RoleType> roleTypes, Pageable pageable);

    Page<User> findByRoleAndStatus(RoleType roleType, String upperCase, Pageable pageable);

    List<User> findByRoleAndDistrict_Id(RoleType role, Long districtId);

    Optional<User> findByTelegramRegistrationCode(String telegramRegistrationCode);
}
