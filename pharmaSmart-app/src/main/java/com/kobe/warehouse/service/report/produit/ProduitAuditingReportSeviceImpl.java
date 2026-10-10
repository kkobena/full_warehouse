package com.kobe.warehouse.service.report.produit;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.stat.impl.SuiviArticleReportReportService;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProduitAuditingReportSeviceImpl implements ProduitAuditingReportSevice {

    private final SuiviArticleReportReportService suiviArticleReportService;

    public ProduitAuditingReportSeviceImpl(SuiviArticleReportReportService suiviArticleReportService) {
        this.suiviArticleReportService = suiviArticleReportService;
    }

    @Override
    public byte[] printToPdf(List<ProduitAuditingState> datas, Produit produit, ReportPeriode reportPeriode) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return suiviArticleReportService.exportToPdf(
            datas,
            String.format(
                "Suivi du produit %s [%s] du %s au %s ",
                produit.getLibelle(),
                produit.getFournisseurProduitPrincipal().getCodeCip(),
                reportPeriode.from().format(dateTimeFormatter),
                reportPeriode.to().format(dateTimeFormatter)
            )
        );
    }
}
