package com.zeylex.denguesense.model;

import com.zeylex.denguesense.model.enums.RoleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Data
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

    @Column(columnDefinition = "integer default 0")
    private int level = 0;

    @Column
    private String district;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
