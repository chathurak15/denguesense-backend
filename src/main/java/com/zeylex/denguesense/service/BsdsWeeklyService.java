package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.BSDSWeekly;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.weather.DistrictWeekWindow;

import java.time.LocalDate;
import java.util.List;

public interface BsdsWeeklyService {

    BSDSWeekly upsertWeeklyBsds(District district, LocalDate weekStartDate, LocalDate weekEndDate);

    int upsertWeeklyBsds(List<DistrictWeekWindow> weeks, List<String> errors);
}
