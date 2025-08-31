package com.kobe.warehouse.service.stat.impl;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatMensuelle;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatMensuelleWrapper;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchats;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatsSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVente;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelle;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleWrapper;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteSummary;
import com.kobe.warehouse.service.dto.OrderBy;
import com.kobe.warehouse.service.dto.ProduitHistoriqueParam;
import com.kobe.warehouse.service.dto.ProduitRecordParamDTO;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingSum;
import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.report.produit.ProduitAuditingReportSevice;
import com.kobe.warehouse.service.stat.ProductStatService;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductStatServiceImpl implements ProductStatService {

    private final ProduitAuditingReportSevice produitAuditingReportSevice;
    private final SalesLineRepository salesLineRepository;
    private final HistoriqueVenteReportReportService historiqueVenteReportReportService;
    private final OrderLineRepository orderLineRepository;
    private final InventoryTransactionService inventoryTransactionService;
    private final ProduitRepository produitRepository;

    public ProductStatServiceImpl(
        ProduitAuditingReportSevice produitAuditingReportSevice,
        FournisseurProduitRepository fournisseurProduitRepository,
        SalesLineRepository salesLineRepository,
        HistoriqueVenteReportReportService historiqueVenteReportReportService,
        OrderLineRepository orderLineRepository,
        InventoryTransactionService inventoryTransactionService,
        ProduitRepository produitRepository
    ) {
        this.produitAuditingReportSevice = produitAuditingReportSevice;
        this.salesLineRepository = salesLineRepository;
        this.historiqueVenteReportReportService = historiqueVenteReportReportService;
        this.orderLineRepository = orderLineRepository;
        this.inventoryTransactionService = inventoryTransactionService;
        this.produitRepository = produitRepository;
    }

    @Override
    public Page<ProductStatRecord> fetchProductStat(ProduitRecordParamDTO produitRecordParam, Pageable pageable) {
        return salesLineRepository.fetchProductStat(
            buildSalesLineSpecification(produitRecordParam),
            produitRecordParam.getFournisseurId(),
            produitRecordParam.getOrder(),
            pageable
        );
    }

    @Override
    @Transactional
    public List<ProductStatParetoRecord> fetch20x80(ProduitRecordParamDTO produitRecordParam) {
        return getTop80PercentProducts(produitRecordParam);
    }

    @Override
    public Resource printToPdf(ProduitAuditingParam produitAuditingParam) throws MalformedURLException {
        return this.produitAuditingReportSevice.printToPdf(
                this.fetchProduitDailyTransaction(produitAuditingParam, Pageable.unpaged()).getContent(),
            produitRepository.getReferenceById(produitAuditingParam.produitId()),
                new ReportPeriode(produitAuditingParam.fromDate(), produitAuditingParam.toDate())
            );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistoriqueProduitVente> getHistoriqueVente(ProduitHistoriqueParam produitHistorique, Pageable pageable) {
        return this.salesLineRepository.getHistoriqueVente(
                produitHistorique.produitId(),
                produitHistorique.startDate(),
                produitHistorique.endDate(),
                getSalesStatuts(),
                pageable
            );
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoriqueProduitAchatMensuelleWrapper> getHistoriqueAchatMensuelle(ProduitHistoriqueParam produitHistorique) {
        List<HistoriqueProduitAchatMensuelle> historiqueProduitAchats = orderLineRepository.getHistoriqueAchatMensuelle(
            produitHistorique.produitId(),
            produitHistorique.startDate(),
            produitHistorique.endDate(),
            OrderStatut.CLOSED.name()
        );
        return historiqueProduitAchats
            .stream()
            .collect(Collectors.groupingBy(HistoriqueProduitAchatMensuelle::getAnnee))
            .entrySet()
            .stream()
            .map(entry ->
                new HistoriqueProduitAchatMensuelleWrapper(
                    entry.getKey(),
                    entry
                        .getValue()
                        .stream()
                        .collect(Collectors.toMap(HistoriqueProduitAchatMensuelle::getMonth, HistoriqueProduitAchatMensuelle::getQuantite))
                )
            )
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistoriqueProduitAchats> getHistoriqueAchat(ProduitHistoriqueParam produitHistorique, Pageable pageable) {
        return this.orderLineRepository.getHistoriqueAchat(
                produitHistorique.produitId(),
                produitHistorique.startDate(),
                produitHistorique.endDate(),
                OrderStatut.CLOSED.name(),
                pageable
            );
    }

    @Override
    public List<HistoriqueProduitVenteMensuelleWrapper> getHistoriqueVenteMensuelle(ProduitHistoriqueParam produitHistorique) {
        List<HistoriqueProduitVenteMensuelle> historiqueProduitVenteMensuelles = salesLineRepository.getHistoriqueVenteMensuelle(
            produitHistorique.produitId(),
            produitHistorique.startDate(),
            produitHistorique.endDate(),
            Set.of(SalesStatut.CLOSED.name(), SalesStatut.CANCELED.name(), SalesStatut.REMOVE.name())
        );
        return historiqueProduitVenteMensuelles
            .stream()
            .collect(Collectors.groupingBy(HistoriqueProduitVenteMensuelle::getAnnee))
            .entrySet()
            .stream()
            .map(entry ->
                new HistoriqueProduitVenteMensuelleWrapper(
                    entry.getKey(),
                    entry
                        .getValue()
                        .stream()
                        .collect(Collectors.toMap(HistoriqueProduitVenteMensuelle::getMonth, HistoriqueProduitVenteMensuelle::getQuantite))
                )
            )
            .toList();
    }

    @Override
    public HistoriqueProduitAchatsSummary getHistoriqueAchatSummary(ProduitHistoriqueParam produitHistorique) {
        return this.orderLineRepository.getHistoriqueAchatSummary(
                produitHistorique.produitId(),
                produitHistorique.startDate(),
                produitHistorique.endDate(),
                OrderStatut.CLOSED.name()
            );
    }

    @Override
    public HistoriqueProduitVenteSummary getHistoriqueVenteSummary(ProduitHistoriqueParam produitHistorique) {
        return this.salesLineRepository.getHistoriqueVenteSummary(
                produitHistorique.produitId(),
                produitHistorique.startDate(),
                produitHistorique.endDate(),
                getSalesStatuts()
            );
    }

    @Override
    public HistoriqueProduitVenteMensuelleSummary getHistoriqueVenteMensuelleSummary(ProduitHistoriqueParam produitHistorique) {
        return this.salesLineRepository.getHistoriqueVenteMensuelleSummary(
                produitHistorique.produitId(),
                produitHistorique.startDate(),
                produitHistorique.endDate(),
                getSalesStatuts()
            );
    }

    private Specification<SalesLine> buildSalesLineSpecification(ProduitRecordParamDTO produitRecordParam) {
        Pair<LocalDate, LocalDate> periode = this.buildPeriode(produitRecordParam);
        Specification<SalesLine> specification = this.salesLineRepository.filterByStatut(getSalesStatutEnum());
        specification = specification.and(this.salesLineRepository.filterByPeriode(periode.getLeft(), periode.getRight()));
        if (nonNull(produitRecordParam.getProduitId())) {
            specification = specification.and(this.salesLineRepository.filterByPeriode(periode.getLeft(), periode.getRight()));
        }
        specification = specification.and(this.salesLineRepository.notImported());
        specification = specification.and(this.salesLineRepository.filterByCa(EnumSet.of(produitRecordParam.getCategorieChiffreAffaire())));

        return specification;
    }

    private ProductStatParetoRecord buildProductStatParetoRecord(Object[] tuple) {
        if (Objects.isNull(tuple) || tuple.length == 0 || Objects.isNull(tuple[0])) return null;

        return new ProductStatParetoRecord(
            tuple[0].toString(),
            tuple[1].toString(),
            Long.valueOf(tuple[2].toString()),
            Long.valueOf(tuple[3].toString()),
            Double.valueOf(tuple[4].toString())
        );
    }

    private List<ProductStatParetoRecord> getTop80PercentProducts(ProduitRecordParamDTO produitRecordParam) {
        Pair<LocalDate, LocalDate> periode = this.buildPeriode(produitRecordParam);
        String caList = CategorieChiffreAffaire.CA.name();
        String statutList = String.join(",", SalesStatut.CLOSED.name(), SalesStatut.CANCELED.name());
        if (nonNull(produitRecordParam.getOrder()) && produitRecordParam.getOrder() == OrderBy.QUANTITY_SOLD) {
            return this.produitRepository.getTopQty80PercentProducts(periode.getLeft(), periode.getRight(), caList, statutList)
                .stream()
                .map(this::buildProductStatParetoRecord)
                .toList();
        }
        return this.produitRepository.getTopAmount80PercentProducts(periode.getLeft(), periode.getRight(), caList, statutList)
            .stream()
            .map(this::buildProductStatParetoRecord)
            .toList();
    }

    @Override
    public List<ProduitAuditingSum> fetchProduitDailyTransactionSum(ProduitAuditingParam produitAuditingParam) {
        return this.inventoryTransactionService.fetchProduitDailyTransactionSum(produitAuditingParam);
    }

    @Override
    public Page<ProduitAuditingState> fetchProduitDailyTransaction(ProduitAuditingParam produitAuditingParam, Pageable pageable) {
        return this.inventoryTransactionService.fetchProduitDailyTransaction(produitAuditingParam, pageable);
    }

    @Override
    public Resource exportHistoriqueVenteToPdf(ProduitHistoriqueParam produitHistorique) {
        var status = Set.of(SalesStatut.CLOSED.name(), SalesStatut.CANCELED.name());
        return this.historiqueVenteReportReportService.exportHistoriqueVenteToPdf(
                this.salesLineRepository.getHistoriqueVente(
                        produitHistorique.produitId(),
                        produitHistorique.startDate(),
                        produitHistorique.endDate(),
                        status,
                        Pageable.unpaged()
                    ).getContent(),
                this.salesLineRepository.getHistoriqueVenteSummary(
                        produitHistorique.produitId(),
                        produitHistorique.startDate(),
                        produitHistorique.endDate(),
                        status
                    ),
                this.produitRepository.findHistoriqueProduitInfo(produitHistorique.produitId()),
                new ReportPeriode(produitHistorique.startDate(), produitHistorique.endDate())
            );
    }

    @Override
    public Resource exportHistoriqueAchatToPdf(ProduitHistoriqueParam produitHistorique) {
        return this.historiqueVenteReportReportService.exportHistoriqueAchatsToPdf(
                this.orderLineRepository.getHistoriqueAchat(
                        produitHistorique.produitId(),
                        produitHistorique.startDate(),
                        produitHistorique.endDate(),
                        OrderStatut.CLOSED.name(),
                        Pageable.unpaged()
                    ).getContent(),
                this.getHistoriqueAchatSummary(produitHistorique),
            this.produitRepository.findHistoriqueProduitInfo(produitHistorique.produitId()),
                new ReportPeriode(produitHistorique.startDate(), produitHistorique.endDate())
            );
    }

    @Override
    public Resource exportHistoriqueVenteMensuelleToPdf(ProduitHistoriqueParam produitHistorique) {
        return this.historiqueVenteReportReportService.exportHistoriqueVenteMensuelleToPdf(
                this.getHistoriqueVenteMensuelle(produitHistorique),
                this.getHistoriqueVenteMensuelleSummary(produitHistorique),
            this.produitRepository.findHistoriqueProduitInfo(produitHistorique.produitId()),
                new ReportPeriode(produitHistorique.startDate(), produitHistorique.endDate())
            );
    }

    @Override
    public Resource exportHistoriqueAchatMensuelToPdf(ProduitHistoriqueParam produitHistorique) {
        return this.historiqueVenteReportReportService.exportHistoriqueAchatsMensuelToPdf(
                this.getHistoriqueAchatMensuelle(produitHistorique),
                this.getHistoriqueAchatSummary(produitHistorique),
            this.produitRepository.findHistoriqueProduitInfo(produitHistorique.produitId()),
                new ReportPeriode(produitHistorique.startDate(), produitHistorique.endDate())
            );
    }

    private Set<String> getSalesStatuts() {
        return Set.of(SalesStatut.CLOSED.name(), SalesStatut.CANCELED.name(), SalesStatut.REMOVE.name());
    }

    private EnumSet<SalesStatut> getSalesStatutEnum() {
        return EnumSet.of(SalesStatut.CLOSED, SalesStatut.CANCELED, SalesStatut.REMOVE);
    }
}
