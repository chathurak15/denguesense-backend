package com.zeylex.denguesense.service;

import com.zeylex.denguesense.weather.DistrictWeekWindow;

import java.util.List;

public interface WeeklyCaseEnrichmentService {

    void enrichImportedWeeks(List<DistrictWeekWindow> weeks);
}
