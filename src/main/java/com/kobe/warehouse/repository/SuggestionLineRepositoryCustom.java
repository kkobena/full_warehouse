package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SuggestionLineRepositoryCustom {



    Page<SuggestionLineDTO> fetchSuggestionLinesWithConsommation(
        Integer suggestionId, String search, Integer storageId, LocalDate dateRetention, int nthMois, Pageable pageable
    );
}

