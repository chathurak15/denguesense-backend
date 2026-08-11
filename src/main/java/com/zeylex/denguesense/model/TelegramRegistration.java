package com.zeylex.denguesense.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "telegram_registration",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_telegram_registration_user", columnNames = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TelegramRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "chat_id", nullable = false)
    private String chatId;

    @Column(name = "district_id")
    private Long districtId;

    @CreationTimestamp
    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;
}
