package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.report.excel.CsvExportService;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import com.kobe.warehouse.service.stock.dto.RecapProduitVendu;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduRequestParam;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kobe.warehouse.service.stock.dto.SeuilFilterType.SEUIL_MINI_ATTEINT;

@Service
@Transactional
public class RecapProduitVenduServiceImpl implements RecapProduitVenduService {
    private static final String[] headers = {
        "ID Produit",
        "Libellé Produit",
        "Code CIP",
        "Code EAN Laboratoire",
        "Rayon",
        "Quantité Vendue",
        "Total ca",
        "Total Achat",
        "Stock actuel",
        "Quantité Avoir"
    };

    private final EntityManager entityManager;
    private final ReportExcelExportService excelExportService;
    private final CsvExportService csvExportService;

    public RecapProduitVenduServiceImpl(EntityManager entityManager, ReportExcelExportService excelExportService, CsvExportService csvExportService) {
        this.entityManager = entityManager;
        this.excelExportService = excelExportService;
        this.csvExportService = csvExportService;
    }

    private QuerySpec buildWhereClause(RecapProduitVenduRequestParam requestParam) {
        StringBuilder where = new StringBuilder(" WHERE COALESCE(s.canceled, false) = false");
        Map<String, Object> params = new HashMap<>();

        // Date/time filtering
        LocalDate startDate = requestParam.startDate();
        LocalDate endDate = requestParam.endDate();
        LocalTime startTime = requestParam.startTime();
        LocalTime endTime = requestParam.endTime();
        if (startDate != null && endDate != null && startTime != null && endTime != null) {
            where.append(" AND sl.created_at BETWEEN :fromDateTime AND :toDateTime");
            params.put("fromDateTime", LocalDateTime.of(startDate, startTime));
            params.put("toDateTime", LocalDateTime.of(endDate, endTime));
        } else if (startDate != null && endDate != null) {
            where.append(" AND s.sale_date BETWEEN :startDate AND :endDate");
            params.put("startDate", startDate);
            params.put("endDate", endDate);
        }

        if (requestParam.userId() != null) {
            where.append(" AND s.caissier_id = :userId");
            params.put("userId", requestParam.userId());
        }
        if (StringUtils.hasText(requestParam.searchTerm())) {
            where.append(
                " AND (UPPER(p.libelle) LIKE :q OR UPPER(p.code_ean_labo) LIKE :q OR UPPER(fp.code_cip) LIKE :q OR UPPER(fp.code_ean) LIKE :q)"
            );
            params.put("q", requestParam.searchTerm().toUpperCase() + "%");
        }
        if (requestParam.rayonId() != null) {
            where.append(" AND r.id = :rayonId");
            params.put("rayonId", requestParam.rayonId());
        }
        if (requestParam.fournisseurId() != null) {
            where.append(" AND fp.fournisseur_id = :fournisseurId");
            params.put("fournisseurId", requestParam.fournisseurId());
        }
        if (requestParam.quantitySold() != null) {
            if (requestParam.quantitySold() == 0) {
                // Pour les produits invendus, on cherche les produits sans ventes
                // Cette condition sera gérée dans la requête HAVING
            } else {
                where.append(" AND sl.quantity_sold = :quantitySold");
                params.put("quantitySold", requestParam.quantitySold());
            }
        }
        if (Boolean.TRUE.equals(requestParam.unitPriceLessThanPurchasePrice())) {
            where.append(" AND  sl.net_unit_price < sl.cost_amount");
        }
        if ((requestParam.seuilFilterType() != null && requestParam.seuilFilterType() != SEUIL_MINI_ATTEINT) && requestParam.seuilValue() != null) {
            switch (requestParam.seuilFilterType()) {
                case EQUAL_TO -> where.append(" AND p.qty_seuil_mini=:seuilValue");
                case GREATER_THAN -> where.append(" AND p.qty_seuil_mini>:seuilValue");
                case LESS_THAN -> where.append(" AND p.qty_seuil_mini<:seuilValue");
                case GREATER_THAN_OR_EQUAL_TO -> where.append("AND p.qty_seuil_mini>=:seuilValue");
                case LESS_THAN_OR_EQUAL_TO -> where.append("AND p.qty_seuil_mini<=:seuilValue");
                default -> where.append(" AND p.qty_seuil_mini<>:seuilValue");// on ne devrait jamais passer ici

            }
            params.put("seuilValue", requestParam.seuilValue());

        }
        if (requestParam.stockFilterType() != null && requestParam.stockValue() != null) {
            switch (requestParam.stockFilterType()) {
                case EQUAL_TO -> where.append(" AND (st.qty_stock + st.qty_ug) = :stockValue");
                case GREATER_THAN -> where.append(" AND (st.qty_stock + st.qty_ug) > :stockValue");
                case LESS_THAN -> where.append(" AND (st.qty_stock + st.qty_ug) < :stockValue");
                case GREATER_THAN_OR_EQUAL_TO -> where.append(" AND (st.qty_stock + st.qty_ug) >= :stockValue");
                case LESS_THAN_OR_EQUAL_TO -> where.append(" AND (st.qty_stock + st.qty_ug) <= :stockValue");
                case NOT_EQUAL_TO -> where.append(" AND (st.qty_stock + st.qty_ug) <> :stockValue");
            }
            params.put("stockValue", requestParam.stockValue());

        }

        return new QuerySpec(where.toString(), params);
    }

    private String buildCountSql(QuerySpec spec, RecapProduitVenduRequestParam requestParam) {
        return "SELECT COUNT(DISTINCT p.id) FROM sales_line sl " +
            " JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date " +
            " JOIN produit p ON p.id = sl.produit_id " +
            " LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id " +
            " JOIN stock_produit st ON st.produit_id = p.id  " +
            " LEFT JOIN rayon_produit rp ON rp.produit_id = p.id " +
            " LEFT JOIN rayon r ON r.id = rp.rayon_id " +
            spec.where + buildHavingClause(requestParam);
    }

    private long getCountTotalPages(QuerySpec spec, RecapProduitVenduRequestParam requestParam) {
        // Count query
        String countSql = buildCountSql(spec, requestParam);

        Query countQuery = entityManager.createNativeQuery(countSql);
        spec.params.forEach(countQuery::setParameter);
        return ((Number) countQuery.getSingleResult()).longValue();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecapProduitVendu> getRecapProduitVenduReport(RecapProduitVenduRequestParam requestParam, Pageable pageable) {
        // Build dynamic native SQL
        StringBuilder sb = new StringBuilder();
        QuerySpec spec = buildWhereClause(requestParam);

        // Base joins (using explicit joins in FROM for better control)
        sb.append("SELECT p.id, p.libelle, p.code_ean_labo, ")
            .append(" COALESCE(MIN(fp.code_cip), '') AS code_cip, ")
            .append(" COALESCE(MIN(r.libelle), '') AS rayon_name, ")
            .append(" SUM(sl.quantity_sold) AS quantity_sold, ")
            .append(" SUM(sl.quantity_avoir) AS quantity_avoir, ")
            .append(" SUM(sl.sales_amount) AS total_sales_amount, ")
            .append(" SUM(sl.cost_amount*sl.quantity_requested) AS total_purchase_amount, ")
            .append(" SUM(st.qty_stock+st.qty_ug) AS total_total_stock ")
            .append(" FROM sales_line sl ")
            .append(" JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date ")
            .append(" JOIN produit p ON p.id = sl.produit_id ")
            .append(" JOIN stock_produit st ON st.produit_id = p.id ")
            .append(" LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id ")
            .append(" LEFT JOIN rayon_produit rp ON rp.produit_id = p.id ")
            .append(" LEFT JOIN rayon r ON r.id = rp.rayon_id ");

        String groupBy = " GROUP BY p.id, p.libelle, p.code_ean_labo";

        String baseFromWhere = sb + spec.where;


        // Data query with pagination and ordering by total sales desc
        String dataSql = baseFromWhere + groupBy + buildHavingClause(requestParam) + " ORDER BY total_sales_amount DESC";
        Query dataQuery = entityManager.createNativeQuery(dataSql);
        spec.params.forEach(dataQuery::setParameter);
        long totalElements = 0;
        if (pageable.isPaged()) {
            dataQuery.setFirstResult((int) pageable.getOffset());
            dataQuery.setMaxResults(pageable.getPageSize());
            totalElements = getCountTotalPages(spec, requestParam);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<RecapProduitVendu> content = new ArrayList<>();
        for (Object[] r : rows) {
            Integer id = r[0] != null ? ((Number) r[0]).intValue() : null;
            String libelle = (String) r[1];
            String codeEanLaboratoire = (String) r[2];
            String codeCip = (String) r[3];
            String rayonName = (String) r[4];
            Integer qtySold = r[5] != null ? ((Number) r[5]).intValue() : 0;
            Integer qtyAvoir = r[6] != null ? ((Number) r[6]).intValue() : 0;
            Integer totalSales = r[7] != null ? ((Number) r[7]).intValue() : 0;
            Integer totalPurchase = r[8] != null ? ((Number) r[8]).intValue() : 0;
            Integer totalTotalStock = r[9] != null ? ((Number) r[9]).intValue() : 0;
            content.add(new RecapProduitVendu(id, libelle, codeCip, codeEanLaboratoire, rayonName, qtySold, qtyAvoir, totalSales, totalPurchase, totalTotalStock));
        }

        return new PageImpl<>(content, pageable, totalElements);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecapProduitVendu> getRecapProduitInvenduReport(RecapProduitVenduRequestParam requestParam, Pageable pageable) {
        // Build query for products with no sales in the period
        StringBuilder sb = new StringBuilder();
        QuerySpec spec = buildWhereClauseForUnsold(requestParam);

        sb.append("SELECT p.id, p.libelle, p.code_ean_labo, ")
            .append(" COALESCE(MIN(fp.code_cip), '') AS code_cip, ")
            .append(" COALESCE(MIN(r.libelle), '') AS rayon_name, ")
            .append(" 0 AS quantity_sold, ")
            .append(" 0 AS quantity_avoir, ")
            .append(" COALESCE(SUM((st.qty_stock + st.qty_ug)*p.regular_unit_price), 0)  AS total_sales_amount, ")
            .append(" COALESCE(SUM((st.qty_stock + st.qty_ug)*p.cost_amount), 0) AS total_purchase_amount, ")
            .append(" COALESCE(SUM(st.qty_stock + st.qty_ug), 0) AS total_stock ")
            .append(" FROM produit p ")
            .append(" JOIN stock_produit st ON st.produit_id = p.id ")
            .append(" LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id ")
            .append(" LEFT JOIN rayon_produit rp ON rp.produit_id = p.id ")
            .append(" LEFT JOIN rayon r ON r.id = rp.rayon_id ")
            .append(" WHERE p.id NOT IN (")
            .append("   SELECT DISTINCT sl.produit_id FROM sales_line sl ")
            .append("   JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date ")
            .append("   WHERE COALESCE(s.canceled, false) = false ");

        // Add date filtering for the subquery
        LocalDate startDate = requestParam.startDate();
        LocalDate endDate = requestParam.endDate();
        if (startDate != null && endDate != null) {
            sb.append(" AND s.sale_date BETWEEN :startDate AND :endDate ");
        }
        sb.append(" ) ");

        // Add additional filters
        sb.append(spec.where);
        sb.append(" GROUP BY p.id, p.libelle, p.code_ean_labo");
        sb.append(" ORDER BY p.libelle");

        String dataSql = sb.toString();
        Query dataQuery = entityManager.createNativeQuery(dataSql);
        spec.params.forEach(dataQuery::setParameter);
        if (startDate != null && endDate != null) {
            dataQuery.setParameter("startDate", startDate);
            dataQuery.setParameter("endDate", endDate);
        }

        long totalElements = 0;
        if (pageable.isPaged()) {
            dataQuery.setFirstResult((int) pageable.getOffset());
            dataQuery.setMaxResults(pageable.getPageSize());
            // Count query
            totalElements = getCountUnsoldProducts(requestParam);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<RecapProduitVendu> content = new ArrayList<>();
        for (Object[] r : rows) {
            Integer id = r[0] != null ? ((Number) r[0]).intValue() : null;
            String libelle = (String) r[1];
            String codeEanLaboratoire = (String) r[2];
            String codeCip = (String) r[3];
            String rayonName = (String) r[4];
            Integer totalSales =  r[7] != null ? ((Number) r[7]).intValue() : 0;
            Integer totalPurchase =  r[8] != null ? ((Number) r[8]).intValue() : 0;
            Integer totalStock = r[9] != null ? ((Number) r[9]).intValue() : 0;
            content.add(new RecapProduitVendu(id, libelle, codeCip, codeEanLaboratoire, rayonName, 0, 0, totalSales, totalPurchase, totalStock));
        }

        return new PageImpl<>(content, pageable, totalElements);
    }

    @Override
    public RecapProduitVenduSummary getRecapProduitVenduSummary(RecapProduitVenduRequestParam requestParam) {
        StringBuilder sb = new StringBuilder();
        QuerySpec spec = buildWhereClause(requestParam);

        sb.append("SELECT COUNT(DISTINCT p.id) AS total_products, ")
            .append(" COALESCE(SUM(sl.quantity_sold),0) AS quantity_sold, ")
            .append(" COALESCE(SUM(sl.quantity_avoir),0) AS quantity_avoir, ")
            .append(" COALESCE(SUM(sl.sales_amount),0) AS total_sales_amount, ")
            .append(" COALESCE(SUM(sl.cost_amount*sl.quantity_requested),0) AS total_purchase_amount, ")
            .append(" COALESCE(SUM(st.qty_stock + st.qty_ug),0) AS total_stock ")
            .append(" FROM sales_line sl ")
            .append(" JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date ")
            .append(" JOIN produit p ON p.id = sl.produit_id ")
            .append(" JOIN stock_produit st ON st.produit_id = p.id ")
            .append(" LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id ")
            .append(" LEFT JOIN rayon_produit rp ON rp.produit_id = p.id ")
            .append(" LEFT JOIN rayon r ON r.id = rp.rayon_id ");

        String sql = sb + spec.where + buildHavingClause(requestParam);
        Query q = entityManager.createNativeQuery(sql);
        spec.params.forEach(q::setParameter);
        Object[] row = (Object[]) q.getSingleResult();
        Long totalProducts = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        Integer qtySold = row[1] != null ? ((Number) row[1]).intValue() : 0;
        Integer qtyAvoir = row[2] != null ? ((Number) row[2]).intValue() : 0;
        Long totalSales = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        Long totalPurchase = row[4] != null ? ((Number) row[4]).longValue() : 0L;
        Long totalStock = row[5] != null ? ((Number) row[5]).longValue() : 0L;
        return new RecapProduitVenduSummary(totalProducts, qtySold, qtyAvoir, totalSales, totalPurchase, totalStock);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToPdf(RecapProduitVenduRequestParam requestParam) {
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(RecapProduitVenduRequestParam requestParam) throws IOException {
        List<RecapProduitVendu> data = getRecapProduitVenduReport(requestParam, Pageable.unpaged()).getContent();

        return excelExportService.createExcelReport(buildReportTitle(requestParam.startDate(), requestParam.endDate()), headers, data, (row, dto) -> {
            row.createCell(0).setCellValue(dto.id().toString());
            row.createCell(1).setCellValue(dto.libelle());
            row.createCell(2).setCellValue(dto.codeCip());
            row.createCell(3).setCellValue(StringUtils.hasText(dto.codeEanLaboratoire()) ? dto.codeEanLaboratoire() : "");
            row.createCell(4).setCellValue(dto.rayonName());
            row.createCell(5).setCellValue(dto.quantitySold());
            row.createCell(6).setCellValue(dto.totalSalesAmount());
            row.createCell(7).setCellValue(dto.totalPurchaseAmount());
            row.createCell(8).setCellValue(dto.stock());
            row.createCell(9).setCellValue(dto.quantityAvoir());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToCsv(RecapProduitVenduRequestParam requestParam) throws IOException {
        List<RecapProduitVendu> data = getRecapProduitVenduReport(requestParam, Pageable.unpaged()).getContent();
        byte[] csvData = csvExportService.createCsvReport(buildReportTitle(requestParam.startDate(), requestParam.endDate()), headers, data, dto -> new String[]{
            dto.id().toString(),
            dto.libelle(),
            dto.codeCip(),
            StringUtils.hasText(dto.codeEanLaboratoire()) ? dto.codeEanLaboratoire() : "",
            dto.rayonName(),
            String.valueOf(dto.quantitySold()),
            String.valueOf(dto.totalSalesAmount()),
            String.valueOf(dto.totalPurchaseAmount()),
            String.valueOf(dto.stock()),
            String.valueOf(dto.quantityAvoir())
        });

        return csvExportService.addUtf8Bom(csvData);
    }

    @Override
    public int createSuggestionFromRecapProduitVendu(RecapProduitVenduRequestParam requestParam) {
        return 0;
    }

    @Override
    public int createInventoryFromRecapProduitVendu(RecapProduitVenduRequestParam requestParam) {
        return 0;
    }

    private String buildHavingClause(RecapProduitVenduRequestParam requestParam) {
        // Having clause for seuil mini atteint
        if (requestParam.seuilFilterType() != null && requestParam.seuilFilterType() == SEUIL_MINI_ATTEINT) {
            return " HAVING (st.qty_stock + st.qty_ug) >= p.qty_seuil_mini ";
        }
        return "";
    }

    private QuerySpec buildWhereClauseForUnsold(RecapProduitVenduRequestParam requestParam) {
        StringBuilder where = new StringBuilder("");
        Map<String, Object> params = new HashMap<>();

        if (requestParam.searchTerm() != null && !requestParam.searchTerm().isBlank()) {
            where.append(
                " AND (UPPER(p.libelle) LIKE :q OR UPPER(p.code_ean_labo) LIKE :q OR UPPER(fp.code_cip) LIKE :q OR UPPER(fp.code_ean) LIKE :q)"
            );
            params.put("q", requestParam.searchTerm().toUpperCase() + "%");
        }
        if (requestParam.rayonId() != null) {
            where.append(" AND r.id = :rayonId");
            params.put("rayonId", requestParam.rayonId());
        }
        if (requestParam.fournisseurId() != null) {
            where.append(" AND fp.fournisseur_id = :fournisseurId");
            params.put("fournisseurId", requestParam.fournisseurId());
        }
        if ((requestParam.seuilFilterType() != null && requestParam.seuilFilterType() != SEUIL_MINI_ATTEINT) && requestParam.seuilValue() != null) {
            switch (requestParam.seuilFilterType()) {
                case EQUAL_TO -> where.append(" AND p.qty_seuil_mini=:seuilValue");
                case GREATER_THAN -> where.append(" AND p.qty_seuil_mini>:seuilValue");
                case LESS_THAN -> where.append(" AND p.qty_seuil_mini<:seuilValue");
                case GREATER_THAN_OR_EQUAL_TO -> where.append(" AND p.qty_seuil_mini>=:seuilValue");
                case LESS_THAN_OR_EQUAL_TO -> where.append(" AND p.qty_seuil_mini<=:seuilValue");
                default -> where.append(" AND p.qty_seuil_mini<>:seuilValue");
            }
            params.put("seuilValue", requestParam.seuilValue());
        }
        if (requestParam.stockFilterType() != null && requestParam.stockValue() != null) {
            switch (requestParam.stockFilterType()) {
                case EQUAL_TO -> where.append(" AND (st.qty_stock + st.qty_ug) = :stockValue");
                case GREATER_THAN -> where.append(" AND (st.qty_stock + st.qty_ug) > :stockValue");
                case LESS_THAN -> where.append(" AND (st.qty_stock + st.qty_ug) < :stockValue");
                case GREATER_THAN_OR_EQUAL_TO -> where.append(" AND (st.qty_stock + st.qty_ug) >= :stockValue");
                case LESS_THAN_OR_EQUAL_TO -> where.append(" AND (st.qty_stock + st.qty_ug) <= :stockValue");
                case NOT_EQUAL_TO -> where.append(" AND (st.qty_stock + st.qty_ug) <> :stockValue");
            }
            params.put("stockValue", requestParam.stockValue());
        }

        return new QuerySpec(where.toString(), params);
    }

    private long getCountUnsoldProducts(RecapProduitVenduRequestParam requestParam) {
        StringBuilder sb = new StringBuilder();
        QuerySpec spec = buildWhereClauseForUnsold(requestParam);

        sb.append("SELECT COUNT(DISTINCT p.id) FROM produit p ")
            .append(" JOIN stock_produit st ON st.produit_id = p.id ")
            .append(" LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id ")
            .append(" LEFT JOIN rayon_produit rp ON rp.produit_id = p.id ")
            .append(" LEFT JOIN rayon r ON r.id = rp.rayon_id ")
            .append(" WHERE p.id NOT IN (")
            .append("   SELECT DISTINCT sl.produit_id FROM sales_line sl ")
            .append("   JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date ")
            .append("   WHERE COALESCE(s.canceled, false) = false ");

        LocalDate startDate = requestParam.startDate();
        LocalDate endDate = requestParam.endDate();
        if (startDate != null && endDate != null) {
            sb.append(" AND s.sale_date BETWEEN :startDate AND :endDate ");
        }
        sb.append(" ) ");
        sb.append(spec.where);

        String countSql = sb.toString();
        Query countQuery = entityManager.createNativeQuery(countSql);
        spec.params.forEach(countQuery::setParameter);
        if (startDate != null && endDate != null) {
            countQuery.setParameter("startDate", startDate);
            countQuery.setParameter("endDate", endDate);
        }

        return ((Number) countQuery.getSingleResult()).longValue();
    }

    private String buildReportTitle(LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return "Recapitulatif des Produits Vendus du " + startDate.format(formatter) + " au " + endDate.format(formatter);
    }

    private List<ProdduitIdQuantity> getProduitIdQuantities(RecapProduitVenduRequestParam requestParam) {
        StringBuilder sb = new StringBuilder();
        QuerySpec spec = buildWhereClause(requestParam);

        sb.append("SELECT DISTINCT p.id, SUM(sl.quantity_sold) AS quantity_sold ")
            .append(" FROM sales_line sl ")
            .append(" JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date ")
            .append(" JOIN produit p ON p.id = sl.produit_id ")
            .append(" JOIN stock_produit st ON st.produit_id = p.id ")
            .append(" LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id ")
            .append(" LEFT JOIN rayon_produit rp ON rp.produit_id = p.id ")
            .append(" LEFT JOIN rayon r ON r.id = rp.rayon_id ");

        String groupBy = " GROUP BY p.id";

        String sql = sb + spec.where + groupBy + buildHavingClause(requestParam);
        Query q = entityManager.createNativeQuery(sql);
        spec.params.forEach(q::setParameter);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<ProdduitIdQuantity> produitIdQuantities = new ArrayList<>();
        for (Object[] r : rows) {
            Integer produitId = r[0] != null ? ((Number) r[0]).intValue() : null;
            Integer quantitySold = r[1] != null ? ((Number) r[1]).intValue() : 0;
            produitIdQuantities.add(new ProdduitIdQuantity(produitId, quantitySold));
        }
        return produitIdQuantities;
    }

    private Set<Integer> getProduitIds(RecapProduitVenduRequestParam requestParam) {
        StringBuilder sb = new StringBuilder();
        QuerySpec spec = buildWhereClause(requestParam);

        sb.append("SELECT DISTINCT p.id ")
            .append(" FROM sales_line sl ")
            .append(" JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date ")
            .append(" JOIN produit p ON p.id = sl.produit_id ")
            .append(" JOIN stock_produit st ON st.produit_id = p.id ")
            .append(" LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id ")
            .append(" LEFT JOIN rayon_produit rp ON rp.produit_id = p.id ")
            .append(" LEFT JOIN rayon r ON r.id = rp.rayon_id ");

        String sql = sb + spec.where + buildHavingClause(requestParam);
        Query q = entityManager.createNativeQuery(sql);
        spec.params.forEach(q::setParameter);
        @SuppressWarnings("unchecked")
        List<Number> rows = q.getResultList();
        Set<Integer> produitIds = new HashSet<>();
        for (Number r : rows) {
            produitIds.add(r.intValue());
        }
        return produitIds;
    }

    private List<ProduitSeuilMini> getProduitSeuilMini(RecapProduitVenduRequestParam requestParam) {
        StringBuilder sb = new StringBuilder();
        QuerySpec spec = buildWhereClause(requestParam);

        sb.append("SELECT DISTINCT p.id, p.qty_seuil_mini ")
            .append(" FROM sales_line sl ")
            .append(" JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date ")
            .append(" JOIN produit p ON p.id = sl.produit_id ")
            .append(" JOIN stock_produit st ON st.produit_id = p.id ")
            .append(" LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id ")
            .append(" LEFT JOIN rayon_produit rp ON rp.produit_id = p.id ")
            .append(" LEFT JOIN rayon r ON r.id = rp.rayon_id ");

        String sql = sb + spec.where + buildHavingClause(requestParam);
        Query q = entityManager.createNativeQuery(sql);
        spec.params.forEach(q::setParameter);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<ProduitSeuilMini> produitSeuilMinis = new ArrayList<>();
        for (Object[] r : rows) {
            Integer produitId = r[0] != null ? ((Number) r[0]).intValue() : null;
            Integer seuilMini = r[1] != null ? ((Number) r[1]).intValue() : 0;
            produitSeuilMinis.add(new ProduitSeuilMini(produitId, seuilMini));
        }
        return produitSeuilMinis;
    }


    @Override
    @Transactional(readOnly = true)
    public RecapProduitVenduSummary getRecapProduitInvenduSummary(RecapProduitVenduRequestParam requestParam) {
        StringBuilder sb = new StringBuilder();
        QuerySpec spec = buildWhereClauseForUnsold(requestParam);

        sb.append("SELECT ")
            .append(" COUNT(DISTINCT p.id) AS total_products, ")
            .append(" COALESCE(SUM(st.qty_stock + st.qty_ug), 0) AS total_stock, ")
            .append(" COALESCE(SUM((st.qty_stock + st.qty_ug)*p.cost_amount), 0) AS total_purchase_amount, ")
            .append(" COALESCE(SUM((st.qty_stock + st.qty_ug)*p.regular_unit_price), 0) AS total_sales_amount ")
            .append(" FROM produit p ")
            .append(" JOIN stock_produit st ON st.produit_id = p.id ")
            .append(" LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id ")
            .append(" LEFT JOIN rayon_produit rp ON rp.produit_id = p.id ")
            .append(" LEFT JOIN rayon r ON r.id = rp.rayon_id ")
            .append(" WHERE p.id NOT IN (")
            .append("   SELECT DISTINCT sl.produit_id FROM sales_line sl ")
            .append("   JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date ")
            .append("   WHERE COALESCE(s.canceled, false) = false ");

        LocalDate startDate = requestParam.startDate();
        LocalDate endDate = requestParam.endDate();
        if (startDate != null && endDate != null) {
            sb.append(" AND s.sale_date BETWEEN :startDate AND :endDate ");
        }
        sb.append(" ) ");
        sb.append(spec.where);

        Query query = entityManager.createNativeQuery(sb.toString());
        if (startDate != null && endDate != null) {
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
        }
        spec.params.forEach(query::setParameter);

        Object[] result = (Object[]) query.getSingleResult();

        return new RecapProduitVenduSummary(
            ((Number) result[0]).longValue(),
            0,
            0,
            ((Number) result[3]).longValue(),
            ((Number) result[2]).longValue(),
            ((Number) result[1]).longValue()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInvenduToExcel(RecapProduitVenduRequestParam requestParam) throws IOException {
        List<RecapProduitVendu> data = getRecapProduitInvenduReport(requestParam, Pageable.unpaged()).getContent();

        String[] headersInvendu = {
            "ID Produit",
            "Libellé Produit",
            "Code CIP",
            "Code EAN Laboratoire",
            "Rayon",
            "Stock actuel"
        };

        List<String[]> rows = new ArrayList<>();
        for (RecapProduitVendu dto : data) {
            rows.add(new String[]{
                String.valueOf(dto.id()),
                dto.libelle(),
                dto.codeCip(),
                dto.codeEanLaboratoire(),
                dto.rayonName(),
                String.valueOf(dto.stock())
            });
        }

        String title = "Recapitulatif des Produits Invendus";
        if (requestParam.startDate() != null && requestParam.endDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            title += " du " + requestParam.startDate().format(formatter) + " au " + requestParam.endDate().format(formatter);
        }

        return excelExportService.createSimpleExcelReport(title, headersInvendu, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInvenduToCsv(RecapProduitVenduRequestParam requestParam) throws IOException {
        List<RecapProduitVendu> data = getRecapProduitInvenduReport(requestParam, Pageable.unpaged()).getContent();

        String[] headersInvendu = {
            "ID Produit",
            "Libellé Produit",
            "Code CIP",
            "Code EAN Laboratoire",
            "Rayon",
            "Stock actuel"
        };

        List<String[]> rows = new ArrayList<>();
        for (RecapProduitVendu dto : data) {
            rows.add(new String[]{
                String.valueOf(dto.id()),
                dto.libelle(),
                dto.codeCip(),
                dto.codeEanLaboratoire(),
                dto.rayonName(),
                String.valueOf(dto.stock())
            });
        }

        String title = "Recapitulatif des Produits Invendus";
        if (requestParam.startDate() != null && requestParam.endDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            title += " du " + requestParam.startDate().format(formatter) + " au " + requestParam.endDate().format(formatter);
        }

        return csvExportService.createSimpleCsvReport(title, headersInvendu, rows);
    }

    private record QuerySpec(String where, Map<String, Object> params) {
    }

    private record ProdduitIdQuantity(Integer produitId, Integer quantitySold) {
    }

    private record ProduitSeuilMini(Integer produitId, Integer seuilMini) {

    }

}
