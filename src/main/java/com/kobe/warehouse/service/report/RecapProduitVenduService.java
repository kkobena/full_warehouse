package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.stock.dto.RecapProduitVendu;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduRequestParam;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;

public interface RecapProduitVenduService {
    Page<RecapProduitVendu> getRecapProduitVenduReport(RecapProduitVenduRequestParam requestParam, Pageable pageable);

    Page<RecapProduitVendu> getRecapProduitInvenduReport(RecapProduitVenduRequestParam requestParam, Pageable pageable);

    RecapProduitVenduSummary getRecapProduitVenduSummary(RecapProduitVenduRequestParam requestParam);

    RecapProduitVenduSummary getRecapProduitInvenduSummary(RecapProduitVenduRequestParam requestParam);

    byte[] exportToPdf(RecapProduitVenduRequestParam requestParam);

    byte[] exportToExcel(RecapProduitVenduRequestParam requestParam) throws IOException;

    byte[] exportToCsv(RecapProduitVenduRequestParam requestParam) throws IOException;

    byte[] exportInvenduToExcel(RecapProduitVenduRequestParam requestParam) throws IOException;

    byte[] exportInvenduToCsv(RecapProduitVenduRequestParam requestParam) throws IOException;

    int createSuggestionFromRecapProduitVendu(RecapProduitVenduRequestParam requestParam);

    int createInventoryFromRecapProduitVendu(RecapProduitVenduRequestParam requestParam);
}
