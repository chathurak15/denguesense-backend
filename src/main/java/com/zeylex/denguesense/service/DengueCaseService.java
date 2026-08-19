package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.requestDTO.DengueCaseSubmitDTO;
import com.zeylex.denguesense.dto.responseDTO.DengueCaseResponseDTO;

public interface DengueCaseService {

    DengueCaseResponseDTO addWeeklyCase(DengueCaseSubmitDTO dto);
}
