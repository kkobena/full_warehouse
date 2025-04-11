package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface SuggestionCustomRepository {
    Page<SuggestionProjection> getAllSuggestion(Specification<Suggestion> specification, Pageable pageable);
}
