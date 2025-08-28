package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.DoughnutChart;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import jakarta.persistence.EntityManager;

import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TaxeServiceImpl implements TaxeService, MvtCommonService {


    private final EntityManager entityManager;
    private final TvaReportReportService tvaReportService;
    private final TaxeSpecification taxeSpecification;
    private final AppConfigurationService appConfigurationService;
    public TaxeServiceImpl(EntityManager entityManager, TvaReportReportService tvaReportService, TaxeSpecification taxeSpecification, AppConfigurationService appConfigurationService) {
        this.entityManager = entityManager;
        this.tvaReportService = tvaReportService;
        this.taxeSpecification = taxeSpecification;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public TaxeWrapperDTO fetchTaxe(MvtParam mvtParam,  boolean toExport) {
        List<TaxeDTO> result = fetchTaxe(mvtParam);
        if (result.isEmpty()) {
            return null;
        }
        TaxeWrapperDTO taxeWrapperDTO = new TaxeWrapperDTO();
        buildFromProjection(result, "daily".equals(mvtParam.getGroupeBy()), taxeWrapperDTO);
        if (toExport) {
            return taxeWrapperDTO;
        }
        taxeWrapperDTO.setChart(buildDoughnutChart(taxeWrapperDTO));
        return taxeWrapperDTO;
    }

    @Override
    public Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException {
        return this.tvaReportService.exportToPdf(
            this.fetchTaxe(mvtParam, true),
            new ReportPeriode(mvtParam.getFromDate(), mvtParam.getToDate()),
            StringUtils.hasText(mvtParam.getGroupeBy()) && "daily".equals(mvtParam.getGroupeBy())
        );
    }

    private List<TaxeDTO> fetchTaxe(MvtParam mvtParam) {
        mvtParam.setExcludeFreeUnit(appConfigurationService.excludeFreeUnit());
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TaxeDTO> cq = cb.createQuery(TaxeDTO.class);
        Root<SalesLine> root = cq.from(SalesLine.class);
        cq.where(taxeSpecification.builder(mvtParam).toPredicate(root, cq, cb));

        boolean daily = "daily".equals(mvtParam.getGroupeBy());

        Expression<Integer> taxValue = root.get(SalesLine_.taxValue);
        Expression<Integer> quantiteValue =mvtParam.isExcludeFreeUnit() ? cb.diff(root.get(SalesLine_.quantityRequested),root.get(SalesLine_.quantityUg)):root.get(SalesLine_.quantityRequested);
        Expression<Long> montantTtc =mvtParam.isExcludeFreeUnit() ? cb.sumAsLong(cb.prod(quantiteValue,root.get(SalesLine_.regularUnitPrice))): cb.sumAsLong(root.get(SalesLine_.salesAmount));
        Expression<Long> montantAchat =  cb.sumAsLong(cb.prod(quantiteValue,root.get(SalesLine_.costAmount)));
       // Expression<Long> montantRemise = cb.sumAsLong(root.get(SalesLine_.discountAmount));
        Expression<Long> amountToBeTakenIntoAccount = cb.sumAsLong(root.get(SalesLine_.amountToBeTakenIntoAccount));

       // Expression<Number> taxAmountExpression = cb.quot(cb.prod(root.get(SalesLine_.salesAmount), root.get(SalesLine_.taxValue)), cb.sum(100, root.get(SalesLine_.taxValue)));

        Expression<Number> montantHt=    cb.ceiling(
            cb.sum(
                cb.quot(
                    cb.prod(quantiteValue,root.get(SalesLine_.regularUnitPrice)),
                    cb.sum(1, cb.quot(root.get(SalesLine_.taxValue), 100.0d))
                )
            )
        );
        if (daily) {
            Expression<LocalDate> mvtDate = cb.function("DATE", LocalDate.class, root.get(SalesLine_.sales).get(Sales_.updatedAt));
            cq.select(cb.construct(TaxeDTO.class, mvtDate, taxValue,  montantTtc, montantAchat, montantHt,  amountToBeTakenIntoAccount));
            cq.groupBy(taxValue, mvtDate);
            cq.orderBy(cb.asc(mvtDate));
        } else {
            cq.select(cb.construct(TaxeDTO.class, taxValue, montantTtc, montantAchat, montantHt,   amountToBeTakenIntoAccount));
            cq.groupBy(taxValue);
            cq.orderBy(cb.asc(taxValue));
        }

        return entityManager.createQuery(cq).getResultList();
    }

    private void buildFromProjection(List<TaxeDTO> projections, boolean groupByDate, TaxeWrapperDTO taxeWrapperDTO) {
        taxeWrapperDTO.setGroupDate(groupByDate);
        projections.forEach(taxeDTO -> {
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
        taxeWrapper.setAmountToBeTakenIntoAccount(taxeWrapper.getAmountToBeTakenIntoAccount() + taxe.getAmountToBeTakenIntoAccount());
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
