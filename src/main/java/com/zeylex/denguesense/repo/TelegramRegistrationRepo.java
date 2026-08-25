package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.TelegramRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TelegramRegistrationRepo extends JpaRepository<TelegramRegistration, Long> {

    Optional<TelegramRegistration> findByUser_Id(Long userId);

    @Query("""
            SELECT r FROM TelegramRegistration r
            JOIN FETCH r.user u
            LEFT JOIN FETCH u.district
            WHERE r.chatId = :chatId
            """)
    Optional<TelegramRegistration> findByChatIdWithUser(@Param("chatId") String chatId);
}
