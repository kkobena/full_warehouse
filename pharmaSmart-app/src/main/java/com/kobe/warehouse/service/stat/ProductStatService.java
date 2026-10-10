package com.kobe.warehouse.service.stat;

import com.kobe.warehouse.service.dto.HistoriqueProduitAchatMensuelleWrapper;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchats;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatsSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVente;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleWrapper;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteSummary;
import com.kobe.warehouse.service.dto.ProduitHistoriqueParam;
import com.kobe.warehouse.service.dto.ProduitRecordParamDTO;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingSum;
import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductStatService extends CommonStatService {
    List<ProduitAuditingState> fetchProduitDailyTransaction(ProduitAuditingParam produitAuditingParam);

    List<ProduitAuditingSum> fetchProduitDailyTransactionSum(ProduitAuditingParam produitAuditingParam);

    byte[] exportToExcel(ProduitAuditingParam produitAuditingParam);

    Page<ProductStatRecord> fetchProductStat(ProduitRecordParamDTO produitRecordParam, Pageable pageable);

    List<ProductStatParetoRecord> fetch20x80(ProduitRecordParamDTO produitRecordParam);

    byte[] printToPdf(ProduitAuditingParam produitAuditingParam);

    Page<HistoriqueProduitVente> getHistoriqueVente(ProduitHistoriqueParam produitHistorique, Pageable pageable);

    List<HistoriqueProduitVenteMensuelleWrapper> getHistoriqueVenteMensuelle(ProduitHistoriqueParam produitHistorique);

    Page<HistoriqueProduitAchats> getHistoriqueAchat(ProduitHistoriqueParam produitHistorique, Pageable pageable);

    List<HistoriqueProduitAchatMensuelleWrapper> getHistoriqueAchatMensuelle(ProduitHistoriqueParam produitHistorique);

    HistoriqueProduitAchatsSummary getHistoriqueAchatSummary(ProduitHistoriqueParam produitHistorique);

    HistoriqueProduitVenteSummary getHistoriqueVenteSummary(ProduitHistoriqueParam produitHistorique);

    HistoriqueProduitVenteMensuelleSummary getHistoriqueVenteMensuelleSummary(ProduitHistoriqueParam produitHistorique);

    byte[] exportHistoriqueVenteToPdf(ProduitHistoriqueParam produitHistorique);

    byte[] exportHistoriqueAchatToPdf(ProduitHistoriqueParam produitHistorique);

    byte[] exportHistoriqueVenteMensuelleToPdf(ProduitHistoriqueParam produitHistorique);

    byte[] exportHistoriqueAchatMensuelToPdf(ProduitHistoriqueParam produitHistorique);
}
