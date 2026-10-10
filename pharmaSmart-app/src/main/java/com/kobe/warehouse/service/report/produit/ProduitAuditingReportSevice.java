package com.kobe.warehouse.service.report.produit;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import java.util.List;

public interface ProduitAuditingReportSevice {
    byte[] printToPdf(List<ProduitAuditingState> datas, Produit produit, ReportPeriode reportPeriode);
}
