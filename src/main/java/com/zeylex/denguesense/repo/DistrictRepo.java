package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DistrictRepo extends JpaRepository<District, Integer> {
    @Query(value = """
        SELECT * FROM district
        ORDER BY centroid <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
        LIMIT 1
        """, nativeQuery = true)
    Optional<District> findNearestByCoordinates(@Param("lat") double lat, @Param("lng") double lng);
}
