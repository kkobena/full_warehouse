package com.kobe.warehouse.service.report.produit;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.stat.impl.SuiviArticleReportService;
import java.net.MalformedURLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ProduitAuditingReportSeviceImpl implements ProduitAuditingReportSevice {

    private final SuiviArticleReportService suiviArticleReportService;

    public ProduitAuditingReportSeviceImpl(SuiviArticleReportService suiviArticleReportService) {
        this.suiviArticleReportService = suiviArticleReportService;
    }

    @Override
    public Resource printToPdf(List<ProduitAuditingState> datas, FournisseurProduit fournisseurProduit, ReportPeriode reportPeriode)
        throws MalformedURLException {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return suiviArticleReportService.exportToPdf(
            datas,
            String.format(
                "Suivi du produit %s [%s] du %s au %s ",
                fournisseurProduit.getProduit().getLibelle(),
                fournisseurProduit.getCodeCip(),
                reportPeriode.from().format(dateTimeFormatter),
                reportPeriode.to().format(dateTimeFormatter)
            )
        );
    }
}
