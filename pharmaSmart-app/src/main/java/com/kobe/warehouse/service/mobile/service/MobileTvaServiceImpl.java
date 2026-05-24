package com.kobe.warehouse.service.mobile.service;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import com.kobe.warehouse.service.mobile.dto.Tva;
import com.kobe.warehouse.service.mobile.dto.TvaItem;
import com.kobe.warehouse.service.utils.NumberUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MobileTvaServiceImpl implements MobileTvaService {

    private final TaxeService taxeService;

    public MobileTvaServiceImpl(TaxeService taxeService) {
        this.taxeService = taxeService;
    }

    @Override
    public Tva getTva(MvtParam mvtParam) {
        TaxeWrapperDTO taxeWrapper = this.taxeService.fetchTaxe(mvtParam.build(), false);

        if (nonNull(taxeWrapper)) {
            long totalTtc = taxeWrapper.getTaxes().stream().mapToLong(t -> t.getMontantTtc()).sum();
            return new Tva(
                taxeWrapper
                    .getTaxes()
                    .stream()
                    .map(tva ->
                        new TvaItem(
                            tva.getCodeTva(),
                            NumberUtil.formatToString(tva.getMontantTtc()),
                            NumberUtil.formatToString(tva.getMontantTaxe()),
                            NumberUtil.formatToString(tva.getMontantHt()),
                            (tva.getMontantTtc() * 100) / (double) totalTtc,
                            null
                        )
                    )
                    .toList()
            );
        }
        return null;
    }
}
