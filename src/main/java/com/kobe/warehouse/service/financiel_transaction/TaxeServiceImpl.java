package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.dto.DoughnutChart;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TaxeServiceImpl implements TaxeService, MvtCommonService {

    private static final Logger log = LoggerFactory.getLogger(TaxeServiceImpl.class);
    private static final String SELECT_TAXE =
        """
        SELECT %s  sl.tax_value as codeTva ,SUM(sl.tax_amount) as montantTaxe,
        SUM(sl.sales_amount) as montantTtc, SUM(sl.cost_amount) as montantAchat,SUM(sl.ht_amount) as montantHt,SUM(sl.discount_amount) as montantRemise,
        SUM(sl.net_amount) as montantNet,SUM(sl.montant_tva_ug) as montantTvaUg,SUM(sl.discount_amount_ug) as montantRemiseUg,SUM(sl.amount_to_be_taken_into_account) as amountToBeTakenIntoAccount,
        SUM(sl.quantity_ug*sl.regular_unit_price) as montantTtcUg FROM sales_line sl JOIN sales s ON s.id=sl.sales_id

        """;
    private static final String DATE_COLUMN = " DATE_FORMAT(s.updated_at, '%Y-%m-%d') AS mvtDate,";
    private static final String GROUP_BY_DATE = " group by sl.tax_value, mvtDate ORDER BY mvtDate";
    private static final String GROUP_BY_TVA_CODE = " group by sl.tax_value ORDER BY sl.tax_value";
    private static final String WHERE_CLAUSE =
        " WHERE DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut IN (%s) AND s.dtype IN (%s) AND s.ca IN (%s) AND sl.to_ignore=?3";
    private final EntityManager entityManager;
    private final TvaReportReportService tvaReportService;

    public TaxeServiceImpl(EntityManager entityManager, TvaReportReportService tvaReportService) {
        this.entityManager = entityManager;
        this.tvaReportService = tvaReportService;
    }

    @Override
    public TaxeWrapperDTO fetchTaxe(MvtParam mvtParam, boolean ignoreSomeTaxe, boolean toExport) {
        List<Tuple> result = fetchTaxe(mvtParam, ignoreSomeTaxe);
        if (result.isEmpty()) {
            return null;
        }
        TaxeWrapperDTO taxeWrapperDTO = new TaxeWrapperDTO();
        buildFromTuple(result, "daily".equals(mvtParam.getGroupeBy()), taxeWrapperDTO);
        if (toExport) {
            return taxeWrapperDTO;
        }
        taxeWrapperDTO.setChart(buildDoughnutChart(taxeWrapperDTO));
        return taxeWrapperDTO;
    }

    @Override
    public Resource exportToPdf(MvtParam mvtParam, boolean ignoreSomeTaxe) throws MalformedURLException {
        return this.tvaReportService.exportToPdf(
                this.fetchTaxe(mvtParam, ignoreSomeTaxe, true),
                new ReportPeriode(mvtParam.getFromDate(), mvtParam.getToDate()),
                StringUtils.hasText(mvtParam.getGroupeBy()) && "daily".equals(mvtParam.getGroupeBy())
            );
    }

    private String buildQuery(MvtParam mvtParam) {
        if (StringUtils.hasText(mvtParam.getGroupeBy()) && "daily".equals(mvtParam.getGroupeBy())) {
            return String.format(SELECT_TAXE, DATE_COLUMN) + buildWhereClause(mvtParam) + GROUP_BY_DATE;
        }
        return String.format(SELECT_TAXE, "") + buildWhereClause(mvtParam) + GROUP_BY_TVA_CODE;
    }

    private String buildWhereClause(MvtParam mvtParam) {
        return this.buildWhereClause(WHERE_CLAUSE, mvtParam);
    }

    private List<Tuple> fetchTaxe(MvtParam mvtParam, boolean ignoreSomeTaxe) {
        try {
            return entityManager
                .createNativeQuery(buildQuery(mvtParam), Tuple.class)
                .setParameter(1, mvtParam.getFromDate())
                .setParameter(2, mvtParam.getToDate())
                .setParameter(3, ignoreSomeTaxe)
                .getResultList();
        } catch (Exception e) {
            log.error("Error while fetching taxe", e);
            return List.of();
        }
    }

    private void buildFromTuple(List<Tuple> tuples, boolean groupByDate, TaxeWrapperDTO taxeWrapperDTO) {
        taxeWrapperDTO.setGroupDate(groupByDate);
        tuples.forEach(t -> {
            TaxeDTO taxeDTO = new TaxeDTO();
            if (groupByDate) {
                taxeDTO.setMvtDate(LocalDate.parse(t.get("mvtDate", String.class)));
            }
            taxeDTO.setCodeTva(t.get("codeTva", Integer.class));
            taxeDTO.setMontantTaxe(t.get("montantTaxe", BigDecimal.class).longValue());
            taxeDTO.setMontantTtc(t.get("montantTtc", BigDecimal.class).longValue());
            taxeDTO.setMontantAchat(t.get("montantAchat", BigDecimal.class).longValue());
            taxeDTO.setMontantHt(t.get("montantHt", BigDecimal.class).longValue());
            taxeDTO.setMontantRemise(t.get("montantRemise", BigDecimal.class).longValue());
            taxeDTO.setMontantNet(t.get("montantNet", BigDecimal.class).longValue());
            taxeDTO.setMontantTvaUg(t.get("montantTvaUg", BigDecimal.class).longValue());
            taxeDTO.setMontantRemiseUg(t.get("montantRemiseUg", BigDecimal.class).longValue());
            taxeDTO.setAmountToBeTakenIntoAccount(t.get("amountToBeTakenIntoAccount", BigDecimal.class).longValue());
            taxeDTO.setMontantTtcUg(t.get("montantTtcUg", BigDecimal.class).longValue());
            updateTaxeWrapper(taxeWrapperDTO, taxeDTO);
        });
    }

    private void updateTaxeWrapper(TaxeWrapperDTO taxeWrapper, TaxeDTO taxe) {
        taxeWrapper.setMontantHt(taxeWrapper.getMontantHt() + taxe.getMontantHt());
        taxeWrapper.setMontantTaxe(taxeWrapper.getMontantTaxe() + taxe.getMontantTaxe());
        taxeWrapper.setMontantTtc(taxeWrapper.getMontantTtc() + taxe.getMontantTtc());
        taxeWrapper.setMontantNet(taxeWrapper.getMontantNet() + taxe.getMontantNet());
        taxeWrapper.setMontantRemise(taxeWrapper.getMontantRemise() + taxe.getMontantRemise());
        taxeWrapper.setMontantAchat(taxeWrapper.getMontantAchat() + taxe.getMontantAchat());
        taxeWrapper.setMontantRemiseUg(taxeWrapper.getMontantRemiseUg() + taxe.getMontantRemiseUg());
        taxeWrapper.setMontantTvaUg(taxeWrapper.getMontantTvaUg() + taxe.getMontantTvaUg());
        taxeWrapper.setAmountToBeTakenIntoAccount(taxeWrapper.getAmountToBeTakenIntoAccount() + taxe.getAmountToBeTakenIntoAccount());
        taxeWrapper.setMontantTtcUg(taxeWrapper.getMontantTtcUg() + taxe.getMontantTtcUg());
        taxeWrapper.getTaxes().add(taxe);
    }

    private DoughnutChart buildDoughnutChart(TaxeWrapperDTO taxeWrapperDTO) {
        List<TaxeDTO> taxes = taxeWrapperDTO.getTaxes();
        taxes.sort(Comparator.comparing(TaxeDTO::getCodeTva));
        List<String> labeles = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        taxes
            .stream()
            .collect(Collectors.groupingBy(TaxeDTO::getCodeTva, Collectors.summingLong(TaxeDTO::getMontantTtc)))
            .forEach((k, v) -> {
                labeles.add(k.toString());
                data.add(v);
            });
        return new DoughnutChart(labeles, data);
    }
}
