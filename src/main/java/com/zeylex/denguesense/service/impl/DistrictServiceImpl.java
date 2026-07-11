package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.DistrictService;
import org.springframework.stereotype.Service;

@Service
public class DistrictServiceImpl implements DistrictService {
    private final DistrictRepo districtRepo;

    public DistrictServiceImpl(DistrictRepo districtRepo) {
        this.districtRepo = districtRepo;
    }

    @Override
    public District findByCoordinates(double latitude, double longitude) {
        return districtRepo.findNearestByCoordinates(latitude, longitude).orElse(null);
    }
}
