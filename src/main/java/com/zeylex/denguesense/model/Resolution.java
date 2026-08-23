package com.zeylex.denguesense.model;

import com.zeylex.denguesense.model.enums.ResolutionAction;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resolutions")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Resolution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by", nullable = false)
    private User resolvedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime resolvedAt;

    @NotBlank(message = "Resolution notes must not be blank")
    @Size(min = 1, max = 1000, message = "Notes must be between 1 and 1000 characters")
    @Column(nullable = false, length = 1000)
    private String notes;

    @NotNull(message = "Resolution action is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResolutionAction action;
}
