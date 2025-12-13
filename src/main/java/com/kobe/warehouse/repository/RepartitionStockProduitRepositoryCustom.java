package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RepartitionStockProduitRepositoryCustom {

    /**
     * Executes a dynamic native query based on the provided SQL and search criteria
     *
     * @param query          the main query SQL
     * @param countQuery     the count query SQL
     * @param searchQueryDto the search criteria containing filter values
     * @param pageable       pagination information
     * @return page of result arrays
     */
    Page<Object[]> findRepartitionStockProduitsDynamic(
        String query,
        String countQuery,
        RepartionSearchQueryDto searchQueryDto,
        Pageable pageable
    );
}
