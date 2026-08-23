package com.zeylex.denguesense.model;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "district")
public class District {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String province;

    private String rdhsZone;

    @Column(name = "rdhs_model_id")
    private Integer rdhsModelId;

    @Column(name = "zone_dry_zone")
    private Boolean zoneDryZone;

    @Column(name = "zone_intermediate_zone")
    private Boolean zoneIntermediateZone;

    @Column(name = "zone_wet_zone")
    private Boolean zoneWetZone;

    @Column(name = "population_density")
    private Double populationDensity;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point centroid;

    @OneToMany(mappedBy = "district")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<User> users;

    @OneToMany(mappedBy = "district")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Report> reports;
}
