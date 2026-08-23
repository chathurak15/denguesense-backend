package com.zeylex.denguesense.model;

import com.zeylex.denguesense.model.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Setter
@Getter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fname;

    @Column(nullable = false)
    private String lname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

    @Column(nullable = false)
    private String status;

    private String image;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private RoleType role;

    // Verification Token / OTP
    @Column(length = 6)
    private String token;

    private LocalDateTime tokenExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    /**
     * One-time code a PHI types as {@code /register <code>} in Telegram.
     * Generation and dashboard display are out of scope for the alert module.
     */
    @Column(name = "telegram_registration_code", unique = true, length = 64)
    private String telegramRegistrationCode;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "resolvedBy", fetch = FetchType.LAZY)
    private List<Resolution> resolution;
}
