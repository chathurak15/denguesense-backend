package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.TelegramRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramRegistrationRepo extends JpaRepository<TelegramRegistration, Long> {

    Optional<TelegramRegistration> findByUser_Id(Long userId);
}
