package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class RepartitionStockProduitRepositoryCustomImpl implements RepartitionStockProduitRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Object[]> findRepartitionStockProduitsDynamic(
        String queryStr,
        String countQueryStr,
        RepartionSearchQueryDto searchQueryDto,
        Pageable pageable
    ) {
        // Execute count query
        Query countQuery = entityManager.createNativeQuery(countQueryStr);
        setQueryParameters(countQuery, searchQueryDto);
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        // Execute main query
        Query query = entityManager.createNativeQuery(queryStr);
        setQueryParameters(query, searchQueryDto);

        // Apply pagination
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return new PageImpl<>(results, pageable, total);
    }

    /**
     * Sets query parameters only if they are not null
     * Avoids code duplication for parameter binding
     */
    private void setQueryParameters(Query query, RepartionSearchQueryDto searchQueryDto) {
        if (searchQueryDto.userId() != null) {
            query.setParameter("userId", searchQueryDto.userId());
        }

        if (searchQueryDto.typeRepartition() != null) {
            query.setParameter("typeRepartition", searchQueryDto.typeRepartition().ordinal());
        }

        if (searchQueryDto.storageId() != null) {
            query.setParameter("storageId", searchQueryDto.storageId());
        }

        if (searchQueryDto.stockProduitId() != null) {
            query.setParameter("stockProduitId", searchQueryDto.stockProduitId());
        }

        if (searchQueryDto.searchTerm() != null && !searchQueryDto.searchTerm().isBlank()) {
            query.setParameter("searchTerm", searchQueryDto.searchTerm());
        }

        if (searchQueryDto.dateDebut() != null) {
            query.setParameter("dateDebut", searchQueryDto.dateDebut());
        }

        if (searchQueryDto.dateFin() != null) {
            query.setParameter("dateFin", searchQueryDto.dateFin());
        }
    }
}
