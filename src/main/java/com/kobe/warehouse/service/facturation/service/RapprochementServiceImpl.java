package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.Banque;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant_;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePayment_;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.service.facturation.dto.EtatRapprochementDto;
import com.kobe.warehouse.service.facturation.dto.LigneRapprochementDto;
import com.kobe.warehouse.service.facturation.dto.RapprochementParams;
import com.kobe.warehouse.service.facturation.dto.ReglementDto;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import com.kobe.warehouse.service.report.pdf.RapprochementPdfReportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RapprochementServiceImpl implements RapprochementService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AppConfigurationService appConfigurationService;
    private final RapprochementPdfReportService rapprochementPdfReportService;
    private final ReportExcelExportService excelExportService;

    @PersistenceContext
    private EntityManager entityManager;

    public RapprochementServiceImpl(
        AppConfigurationService appConfigurationService,
        RapprochementPdfReportService rapprochementPdfReportService,
        ReportExcelExportService excelExportService
    ) {
        this.appConfigurationService = appConfigurationService;
        this.rapprochementPdfReportService = rapprochementPdfReportService;
        this.excelExportService = excelExportService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EtatRapprochementDto> getEtatRapprochement(RapprochementParams params, Pageable pageable) {
        List<FactureTiersPayant> factures = queryFactures(params);

        // Group by tiersPayant
        Map<Integer, List<FactureTiersPayant>> grouped = factures.stream()
            .collect(Collectors.groupingBy(f -> {
                if (f.getTiersPayant() != null) return f.getTiersPayant().getId();
                return f.getGroupeTiersPayant() != null ? f.getGroupeTiersPayant().getId() : 0;
            }));

        List<EtatRapprochementDto> result = new ArrayList<>();
        for (Map.Entry<Integer, List<FactureTiersPayant>> entry : grouped.entrySet()) {
            List<FactureTiersPayant> tpFactures = entry.getValue();
            FactureTiersPayant first = tpFactures.getFirst();
            TiersPayant tp = first.getTiersPayant();
            String tpName = tp != null ? tp.getFullName() : (first.getGroupeTiersPayant() != null ? first.getGroupeTiersPayant().getName() : "");
            int delai = tp != null && tp.getDelaiReglement() != null ? tp.getDelaiReglement() : appConfigurationService.getDelaiReglement();

            BigDecimal totalFacture = BigDecimal.ZERO;
            BigDecimal totalRegle = BigDecimal.ZERO;
            List<LigneRapprochementDto> lignes = new ArrayList<>();

            for (FactureTiersPayant f : tpFactures) {
                BigDecimal montantFacture = f.getMontantNet() != null ? f.getMontantNet() : BigDecimal.ZERO;
                BigDecimal montantRegle = BigDecimal.valueOf(f.getMontantRegle());
                BigDecimal ecart = montantFacture.subtract(montantRegle);
                LocalDate echeance = f.getInvoiceDate().plusDays(delai);

                totalFacture = totalFacture.add(montantFacture);
                totalRegle = totalRegle.add(montantRegle);


                List<ReglementDto> reglements = buildPaymentsDtos(f);

                lignes.add(new LigneRapprochementDto(
                    f.getId().getId(),
                    f.getNumFacture(),
                    f.getInvoiceDate(),
                    echeance,
                    montantFacture,
                    montantRegle,
                    ecart,
                    f.getStatut(),
                    reglements
                ));
            }

            BigDecimal ecartTotal = totalFacture.subtract(totalRegle);
            LocalDate debutPeriode = params.startDate();
            LocalDate finPeriode = params.endDate();

            result.add(new EtatRapprochementDto(
                tpName,
                debutPeriode,
                finPeriode,
                totalFacture,
                totalRegle,
                ecartTotal,
                lignes
            ));
        }

        int total = result.size();
        int fromIndex = (int) pageable.getOffset();
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        List<EtatRapprochementDto> pageContent = fromIndex >= total ? Collections.emptyList() : result.subList(fromIndex, toIndex);
        return new PageImpl<>(pageContent, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(RapprochementParams params) {
        List<EtatRapprochementDto> etats = buildAllEtats(params);
        return rapprochementPdfReportService.export(etats, params);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcel(RapprochementParams params) {
        List<EtatRapprochementDto> etats = buildAllEtats(params);

        String[] headers = {
            "Organisme", "N° Facture", "Date facture", "Échéance",
            "Facturé (FCFA)", "Réglé (FCFA)", "Écart (FCFA)", "Statut"
        };

        // Flatten : une ligne par facture
        List<String[]> rows = new ArrayList<>();
        for (EtatRapprochementDto etat : etats) {
            for (LigneRapprochementDto ligne : etat.lignes()) {
                rows.add(new String[]{
                    etat.tiersPayantName(),
                    ligne.numFacture(),
                    ligne.invoiceDate() != null ? ligne.invoiceDate().format(DATE_FMT) : "",
                    ligne.echeance() != null ? ligne.echeance().format(DATE_FMT) : "",
                    ligne.montantFacture() != null ? ligne.montantFacture().toPlainString() : "0",
                    ligne.montantRegle() != null ? ligne.montantRegle().toPlainString() : "0",
                    ligne.ecart() != null ? ligne.ecart().toPlainString() : "0",
                    ligne.statut() != null ? ligne.statut().name() : ""
                });
            }
        }

        try {
            return excelExportService.createSimpleExcelReport("État de Rapprochement", headers, rows);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération Excel du rapprochement", e);
        }
    }

    private List<EtatRapprochementDto> buildAllEtats(RapprochementParams params) {
        List<FactureTiersPayant> factures = queryFactures(params);
        Map<Integer, List<FactureTiersPayant>> grouped = factures.stream()
            .collect(Collectors.groupingBy(f -> {
                if (f.getTiersPayant() != null) return f.getTiersPayant().getId();
                return f.getGroupeTiersPayant() != null ? f.getGroupeTiersPayant().getId() : 0;
            }));

        List<EtatRapprochementDto> result = new ArrayList<>();
        for (Map.Entry<Integer, List<FactureTiersPayant>> entry : grouped.entrySet()) {
            List<FactureTiersPayant> tpFactures = entry.getValue();
            FactureTiersPayant first = tpFactures.get(0);
            TiersPayant tp = first.getTiersPayant();
            String tpName = tp != null ? tp.getFullName()
                : (first.getGroupeTiersPayant() != null ? first.getGroupeTiersPayant().getName() : "");
            int delai = tp != null && tp.getDelaiReglement() != null ? tp.getDelaiReglement() : 30;

            BigDecimal totalFacture = BigDecimal.ZERO;
            BigDecimal totalRegle = BigDecimal.ZERO;
            List<LigneRapprochementDto> lignes = new ArrayList<>();

            for (FactureTiersPayant f : tpFactures) {
                BigDecimal montantFacture = f.getMontantNet() != null ? f.getMontantNet() : BigDecimal.ZERO;
                BigDecimal montantRegle = BigDecimal.valueOf(f.getMontantRegle());
                BigDecimal ecart = montantFacture.subtract(montantRegle);
                LocalDate echeance = f.getInvoiceDate().plusDays(delai);
                totalFacture = totalFacture.add(montantFacture);
                totalRegle = totalRegle.add(montantRegle);
                lignes.add(new LigneRapprochementDto(
                    f.getId().getId(), f.getNumFacture(), f.getInvoiceDate(), echeance,
                    montantFacture, montantRegle, ecart, f.getStatut(), buildPaymentsDtos(f)
                ));
            }
            result.add(new EtatRapprochementDto(
                tpName, params.startDate(), params.endDate(),
                totalFacture, totalRegle, totalFacture.subtract(totalRegle), lignes
            ));
        }
        return result;
    }

    private List<FactureTiersPayant> queryFactures(RapprochementParams params) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FactureTiersPayant> cq = cb.createQuery(FactureTiersPayant.class);
        Root<FactureTiersPayant> root = cq.from(FactureTiersPayant.class);
        List<Predicate> predicates = new ArrayList<>();

        if (params.startDate() != null && params.endDate() != null) {
            predicates.add(cb.between(root.get(FactureTiersPayant_.invoiceDate), params.startDate(), params.endDate()));
        }
        if (params.tiersPayantId() != null) {
            predicates.add(cb.equal(root.get(FactureTiersPayant_.tiersPayant).get(TiersPayant_.id), params.tiersPayantId()));
        }
        if (!CollectionUtils.isEmpty(params.statuts())) {
            predicates.add(root.get(FactureTiersPayant_.statut).in(params.statuts()));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(root.get(FactureTiersPayant_.invoiceDate)));

        TypedQuery<FactureTiersPayant> query = entityManager.createQuery(cq);
        return query.getResultList();
    }

    private List<ReglementDto> buildPaymentsDtos(FactureTiersPayant facture) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<InvoicePayment> cq = cb.createQuery(InvoicePayment.class);
        Root<InvoicePayment> root = cq.from(InvoicePayment.class);
        cq.where(
            cb.equal(root.get(InvoicePayment_.factureTiersPayant).get(FactureTiersPayant_.id), facture.getId().getId()),
            cb.equal(root.get(InvoicePayment_.factureTiersPayant).get(FactureTiersPayant_.invoiceDate), facture.getInvoiceDate())
        );
        List<InvoicePayment> payments = entityManager.createQuery(cq).getResultList();

        return payments.stream().map(this::toReglementDto).collect(Collectors.toList());
    }

    private ReglementDto toReglementDto(InvoicePayment p) {
        String paymentMode = p.getPaymentMode() != null ? p.getPaymentMode().getCode() : null;
        Banque banque = p.getBanque();
        String banqueNom = banque != null ? banque.getNom() : null;
        return new ReglementDto(
            p.getId().getId(),
            p.getTransactionDate(),
            p.getPaidAmount(),
            p.getTransactionNumber(),
            paymentMode,
            banqueNom,
            p.getCommentaire()
        );
    }
}
