package com.zeylex.denguesense.model;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
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
