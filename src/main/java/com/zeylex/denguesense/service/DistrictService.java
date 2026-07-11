package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.District;

public interface DistrictService {
    District findByCoordinates(double latitude, double longitude);
}
