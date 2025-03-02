package com.kobe.warehouse.service.report.produit;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import java.net.MalformedURLException;
import java.util.List;
import org.springframework.core.io.Resource;

public interface ProduitAuditingReportSevice {
    Resource printToPdf(List<ProduitAuditingState> datas, FournisseurProduit fournisseurProduit, ReportPeriode reportPeriode)
        throws MalformedURLException;
}
