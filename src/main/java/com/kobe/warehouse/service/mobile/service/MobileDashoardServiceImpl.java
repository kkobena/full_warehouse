package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.mobile.dto.ListItem;
import com.kobe.warehouse.service.mobile.dto.Dashboard;
import com.kobe.warehouse.service.mobile.dto.KeyValue;
import com.kobe.warehouse.service.stat.AchatStatService;
import com.kobe.warehouse.service.stat.SaleStatService;
import com.kobe.warehouse.service.utils.NumberUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class MobileDashoardServiceImpl implements MobileDashoardService {

    private final SaleStatService saleStatService;
    private final AchatStatService achatStatService;

    public MobileDashoardServiceImpl(SaleStatService saleStatService, AchatStatService achatStatService) {
        this.saleStatService = saleStatService;
        this.achatStatService = achatStatService;
    }

    @Override
    public Dashboard getData(VenteRecordParamDTO venteRecordParam) {
        //pour le mobile, on ne peut pas avoir de date de fin différente de la date de début
        VenteRecordWrapper venteRecordWrapper = saleStatService.getPeridiqueCa(venteRecordParam);
        Dashboard dashboard = buildSales(venteRecordWrapper);
        if (Objects.nonNull(dashboard)) {
            venteRecordParam.setCanceled(false);
            List<VenteByTypeRecord> venteByTypeRecords = saleStatService.getCaGroupingByType(venteRecordParam);
            buildSalesByType(dashboard, venteByTypeRecords);
            List<VenteModePaimentRecord> venteModePaimentRecords = saleStatService.getCaGroupingByPaimentMode(venteRecordParam);
            buildByMode(dashboard, venteModePaimentRecords);
        }

        AchatRecord achatRecord = achatStatService.getAchatPeriode(new AchatRecordParamDTO());

        return achats(dashboard, achatRecord);
    }

    private void buildSalesByType(Dashboard dashboard, List<VenteByTypeRecord> venteByTypeRecords) {
        if (CollectionUtils.isEmpty(venteByTypeRecords)) {
            return;
        }
        List<KeyValue> salesTypes = new ArrayList<>();
        venteByTypeRecords.forEach(venteByTypeRecord -> {
            VenteRecord venteRecord = venteByTypeRecord.venteRecord();
            salesTypes.add(new KeyValue(venteByTypeRecord.typeVente(), NumberUtil.formatToString(venteRecord.salesAmount())));
            salesTypes.add(new KeyValue("Nbre vente " + venteByTypeRecord.typeVente(), NumberUtil.formatToString(venteRecord.saleCount())));
        });
        dashboard.setSalesTypes(salesTypes);
    }

    private void buildByMode(Dashboard dashboard, List<VenteModePaimentRecord> venteModePaimentRecords) {
        if (CollectionUtils.isEmpty(venteModePaimentRecords)) {
            return;
        }
        List<ListItem> modes = new ArrayList<>();
        long total = venteModePaimentRecords.stream()
            .map(VenteModePaimentRecord::paidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add).longValue();
        venteModePaimentRecords.forEach(venteByTypeRecord -> modes.add(new ListItem(venteByTypeRecord.libelle(), NumberUtil.formatToString(venteByTypeRecord.paidAmount()), BigDecimal.valueOf(((venteByTypeRecord.paidAmount().doubleValue() * 100) / total)).round(new MathContext(2, RoundingMode.HALF_UP)).doubleValue() + "")));
        dashboard.setPaymentModes(modes);
    }

    private Dashboard achats(Dashboard dashboard, AchatRecord achatRecord) {
        if (Objects.isNull(achatRecord)) {
            return dashboard;
        }
        String montantTtc = "Montant TTC";
        String montantHt = "Montant HT";
        String montantTva = "TVA";
        String fournisseur = "Nbre d'achats";
        long montantTtcValue = achatRecord.ttcAmount();
        long montantHtValue = achatRecord.receiptAmount().longValue();
        long montantTvaValue = achatRecord.taxAmount().longValue();
        long achatCount = achatRecord.achatCount();
        dashboard = Objects.requireNonNullElse(dashboard, new Dashboard());
        dashboard.setCommandes(
            List.of(
                new KeyValue(montantTtc, NumberUtil.formatToString(montantTtcValue)),
                new KeyValue(montantHt, NumberUtil.formatToString(montantHtValue)),
                new KeyValue(montantTva, NumberUtil.formatToString(montantTvaValue)),
                new KeyValue(fournisseur, NumberUtil.formatToString(achatCount))
            )
        );
        return dashboard;
    }

    private Dashboard buildSales(VenteRecordWrapper venteRecordWrapper) {
        if (Objects.isNull(venteRecordWrapper)) {
            return null;
        }
        String montantTtc = "Montant TTC";
        String montantHt = "Montant HT";
        String montantTva = "TVA";
        String client = "Nbre de ventes";
        long montantTtcValue = 0;
        long montantHtValue = 0;
        long montantRemiseValue = 0;
        long montantTvaValue = 0;
        long saleCount = 0;
        long marge = 0;
        long netAmount = 0;
        long panierMoyen = 0;
        long paidAmount = 0;
        long partTierspayant = 0;
        long restant = 0;
        VenteRecord close = venteRecordWrapper.close();
        if (Objects.nonNull(close)) {
            montantTtcValue = close.salesAmount().longValue();
            montantHtValue = close.htAmount().longValue();
            montantTvaValue = close.taxAmount().longValue();
            saleCount = close.saleCount();
            montantRemiseValue = close.discountAmount().longValue();
            marge = close.marge().longValue();
            netAmount = close.netAmount().longValue();
            paidAmount = close.paidAmount().longValue();
            partTierspayant = close.partTiersPayant().longValue();
            restant = close.restToPay().longValue();
        }
        VenteRecord canceled = venteRecordWrapper.canceled();
        if (Objects.nonNull(canceled)) {
            montantTtcValue += canceled.salesAmount().longValue();
            montantHtValue += canceled.htAmount().longValue();
            montantTvaValue += canceled.taxAmount().longValue();
            saleCount += canceled.saleCount();
            montantRemiseValue += canceled.discountAmount().longValue();
            marge += canceled.marge().longValue();
            netAmount += canceled.netAmount().longValue();
            paidAmount += canceled.paidAmount().longValue();
            partTierspayant += canceled.partTiersPayant().longValue();
            restant += canceled.restToPay().longValue();
        }
        if (saleCount > 0) {
            panierMoyen = netAmount / saleCount;
        }
        if (Objects.isNull(close) && Objects.isNull(canceled)) {
            return null;
        }
        Dashboard dashboard = new Dashboard();
        dashboard.setSales(
            List.of(
                new KeyValue(montantTtc, NumberUtil.formatToString(montantTtcValue)),
                new KeyValue(montantHt, NumberUtil.formatToString(montantHtValue)),
                new KeyValue(montantTva, NumberUtil.formatToString(montantTvaValue)),
                new KeyValue("Panier moyen", NumberUtil.formatToString(panierMoyen)),
                new KeyValue(client, NumberUtil.formatToString(saleCount))
            )
        );
        dashboard.setNetAmounts(
            List.of(
                new KeyValue("Remise", NumberUtil.formatToString(montantRemiseValue)),
                new KeyValue("Montant Net", NumberUtil.formatToString(netAmount)),
                new KeyValue("Comptant", NumberUtil.formatToString(paidAmount)),
                new KeyValue("Crédit", NumberUtil.formatToString((partTierspayant + restant))),
                new KeyValue("Marge", NumberUtil.formatToString(marge))
            )
        );
        return dashboard;
    }
}
