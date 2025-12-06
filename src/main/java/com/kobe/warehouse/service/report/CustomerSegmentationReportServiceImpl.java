package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.CustomerSegmentationDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CustomerSegmentationReportServiceImpl implements CustomerSegmentationReportService {
    private final EntityManager entityManager;

    public CustomerSegmentationReportServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Cacheable(value = "customerSegmentation", key = "'all'")
    public List<CustomerSegmentationDTO> getAllCustomerSegmentation() {
        String sql =
            "SELECT " +
                "customer_id, " +
                "customer_name, " +
                "phone, " +
                "last_purchase_date, " +
                "days_since_last_purchase, " +
                "nb_purchases_last_year, " +
                "total_spent_last_year, " +
                "avg_basket_value, " +
                "recency_score, " +
                "frequency_score, " +
                "monetary_score, " +
                "rfm_segment, " +
                "customer_classification " +
                "FROM mv_customer_rfm " +
                "ORDER BY rfm_segment DESC";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "customerSegmentation", key = "'classification:' + #classification")
    public List<CustomerSegmentationDTO> getCustomersByClassification(
        CustomerSegmentationDTO.CustomerClassification classification
    ) {
        String sql =
            "SELECT " +
                "customer_id, " +
                "customer_name, " +
                "phone, " +
                "last_purchase_date, " +
                "days_since_last_purchase, " +
                "nb_purchases_last_year, " +
                "total_spent_last_year, " +
                "avg_basket_value, " +
                "recency_score, " +
                "frequency_score, " +
                "monetary_score, " +
                "rfm_segment, " +
                "customer_classification " +
                "FROM mv_customer_rfm " +
                "WHERE customer_classification = :classification " +
                "ORDER BY rfm_segment DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("classification", classification.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "customerSegmentation", key = "'champions'")
    public List<CustomerSegmentationDTO> getChampionCustomers() {
        return getCustomersByClassification(CustomerSegmentationDTO.CustomerClassification.CHAMPION);
    }

    @Override
    @Cacheable(value = "customerSegmentation", key = "'at-risk'")
    public List<CustomerSegmentationDTO> getAtRiskCustomers() {
        String sql =
            "SELECT " +
                "customer_id, " +
                "customer_name, " +
                "phone, " +
                "last_purchase_date, " +
                "days_since_last_purchase, " +
                "nb_purchases_last_year, " +
                "total_spent_last_year, " +
                "avg_basket_value, " +
                "recency_score, " +
                "frequency_score, " +
                "monetary_score, " +
                "rfm_segment, " +
                "customer_classification " +
                "FROM mv_customer_rfm " +
                "WHERE customer_classification IN ('AT_RISK', 'NEED_ATTENTION') " +
                "ORDER BY total_spent_last_year DESC";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    public Map<CustomerSegmentationDTO.CustomerClassification, Long> getCustomerCountByClassification() {
        String countQuery = "SELECT customer_classification, COUNT(*) as count FROM mv_customer_rfm  GROUP BY customer_classification";

        Query query = entityManager.createNativeQuery(countQuery);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<CustomerSegmentationDTO.CustomerClassification, Long> counts = new EnumMap<>(
            CustomerSegmentationDTO.CustomerClassification.class
        );

        // Initialize with zeros
        counts.put(CustomerSegmentationDTO.CustomerClassification.CHAMPION, 0L);
        counts.put(CustomerSegmentationDTO.CustomerClassification.LOYAL, 0L);
        counts.put(CustomerSegmentationDTO.CustomerClassification.BIG_SPENDER, 0L);
        counts.put(CustomerSegmentationDTO.CustomerClassification.ACTIVE, 0L);
        counts.put(CustomerSegmentationDTO.CustomerClassification.AT_RISK, 0L);
        counts.put(CustomerSegmentationDTO.CustomerClassification.NEED_ATTENTION, 0L);
        counts.put(CustomerSegmentationDTO.CustomerClassification.INACTIVE, 0L);

        // Fill with actual counts
        for (Object[] row : results) {
            String classificationStr = (String) row[0];
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            try {
                CustomerSegmentationDTO.CustomerClassification classification =
                    CustomerSegmentationDTO.CustomerClassification.valueOf(classificationStr);
                counts.put(classification, count);
            } catch (IllegalArgumentException e) {
                // Ignore invalid classifications
            }
        }

        return counts;
    }

    @Override
    public CustomerSegmentationDTO getCustomerSegmentation(Integer customerId) {
        String sql =
            "SELECT " +
                "customer_id, " +
                "customer_name, " +
                "phone, " +
                "last_purchase_date, " +
                "days_since_last_purchase, " +
                "nb_purchases_last_year, " +
                "total_spent_last_year, " +
                "avg_basket_value, " +
                "recency_score, " +
                "frequency_score, " +
                "monetary_score, " +
                "rfm_segment, " +
                "customer_classification " +
                "FROM mv_customer_rfm " +
                "WHERE customer_id = :customerId";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("customerId", customerId);

        try {
            Object[] result = (Object[]) query.getSingleResult();
            return mapRowToDTO(result);
        } catch (NoResultException e) {
            return null;
        }
    }


    private List<CustomerSegmentationDTO> mapResultsToDTO(List<Object[]> results) {
        return results.stream().map(this::mapRowToDTO).toList();
    }

    private CustomerSegmentationDTO mapRowToDTO(Object[] row) {
        Integer customerId = row[0] != null ? ((Number) row[0]).intValue() : null;
        String customerName = (String) row[1];
        String phone = (String) row[2];
        LocalDate lastPurchaseDate = row[3] != null ? ((Date) row[3]).toLocalDate() : null;
        Integer daysSinceLastPurchase = row[4] != null ? ((Number) row[4]).intValue() : 0;
        Integer nbPurchasesLastYear = row[5] != null ? ((Number) row[5]).intValue() : 0;
        Integer totalSpentLastYear = row[6] != null ? ((Number) row[6]).intValue() : 0;
        BigDecimal avgBasketValue = row[7] != null ? new BigDecimal(row[7].toString()) : BigDecimal.ZERO;
        Integer recencyScore = row[8] != null ? ((Number) row[8]).intValue() : 0;
        Integer frequencyScore = row[9] != null ? ((Number) row[9]).intValue() : 0;
        Integer monetaryScore = row[10] != null ? ((Number) row[10]).intValue() : 0;
        Integer rfmSegment = row[11] != null ? ((Number) row[11]).intValue() : 0;
        String classificationStr = (String) row[12];

        CustomerSegmentationDTO.CustomerClassification classification = classificationStr != null
            ? CustomerSegmentationDTO.CustomerClassification.valueOf(classificationStr)
            : null;

        return new CustomerSegmentationDTO(
            customerId,
            customerName,
            phone,
            lastPurchaseDate,
            daysSinceLastPurchase,
            nbPurchasesLastYear,
            totalSpentLastYear,
            avgBasketValue,
            recencyScore,
            frequencyScore,
            monetaryScore,
            rfmSegment,
            classification
        );
    }
}
