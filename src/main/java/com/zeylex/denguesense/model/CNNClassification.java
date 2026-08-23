package com.zeylex.denguesense.model;

import com.zeylex.denguesense.model.enums.RiskLabel;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cnn_classification")
public class CNNClassification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "report_id", unique = true, nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Report report;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskLabel riskLabel;

    @DecimalMin("0.0") @DecimalMax("1.0")
    @Column(nullable = false)
    private double confidenceScore;

    @Column(nullable = false)
    private String modelVersion;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime classifiedAt;
}
