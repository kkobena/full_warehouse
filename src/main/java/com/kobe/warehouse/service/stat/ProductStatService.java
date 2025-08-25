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
import java.net.MalformedURLException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductStatService extends CommonStatService {
    Page<ProduitAuditingState> fetchProduitDailyTransaction(ProduitAuditingParam produitAuditingParam, Pageable pageable);

    List<ProduitAuditingSum> fetchProduitDailyTransactionSum(ProduitAuditingParam produitAuditingParam);

    Page<ProductStatRecord> fetchProductStat(ProduitRecordParamDTO produitRecordParam, Pageable pageable);

    List<ProductStatParetoRecord> fetch20x80(ProduitRecordParamDTO produitRecordParam);

    Resource printToPdf(ProduitAuditingParam produitAuditingParam) throws MalformedURLException;

    Page<HistoriqueProduitVente> getHistoriqueVente(ProduitHistoriqueParam produitHistorique, Pageable pageable);

    List<HistoriqueProduitVenteMensuelleWrapper> getHistoriqueVenteMensuelle(ProduitHistoriqueParam produitHistorique);

    Page<HistoriqueProduitAchats> getHistoriqueAchat(ProduitHistoriqueParam produitHistorique, Pageable pageable);

    List<HistoriqueProduitAchatMensuelleWrapper> getHistoriqueAchatMensuelle(ProduitHistoriqueParam produitHistorique);

    HistoriqueProduitAchatsSummary getHistoriqueAchatSummary(ProduitHistoriqueParam produitHistorique);

    HistoriqueProduitVenteSummary getHistoriqueVenteSummary(ProduitHistoriqueParam produitHistorique);

    HistoriqueProduitVenteMensuelleSummary getHistoriqueVenteMensuelleSummary(ProduitHistoriqueParam produitHistorique);

    Resource exportHistoriqueVenteToPdf(ProduitHistoriqueParam produitHistorique);

    Resource exportHistoriqueAchatToPdf(ProduitHistoriqueParam produitHistorique);

    Resource exportHistoriqueVenteMensuelleToPdf(ProduitHistoriqueParam produitHistorique);

    Resource exportHistoriqueAchatMensuelToPdf(ProduitHistoriqueParam produitHistorique);
}
