package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.dto.records.Tuple;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseService;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.mobile.dto.Balance;
import com.kobe.warehouse.service.mobile.dto.BalanceCard;
import com.kobe.warehouse.service.mobile.dto.BalanceWrapper;
import com.kobe.warehouse.service.mobile.dto.CartName;
import com.kobe.warehouse.service.utils.NumberUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Service
@Transactional(readOnly = true)
public class MobileBalanceServiceImpl implements MobileBalanceService {
    private final BalanceCaisseService balanceCaisseService;

    public MobileBalanceServiceImpl(BalanceCaisseService balanceCaisseService) {
        this.balanceCaisseService = balanceCaisseService;
    }

    @Override
    public BalanceWrapper getBalanceCaisse(MvtParam mvtParam) {
        BalanceCaisseWrapper balanceCaisseWrapper = this.balanceCaisseService.getBalanceCaisse(mvtParam.build());
        List<BalanceCard> items = new ArrayList<>();
        if (nonNull(balanceCaisseWrapper)) {
            buildSammary(balanceCaisseWrapper, items);
            buildTypeVente(items, balanceCaisseWrapper.getBalanceCaisses());
            buildMvt(items, balanceCaisseWrapper.getMvtCaisses());
            buildModeRegelement(items, balanceCaisseWrapper.getMvtCaissesByModes());

        }

        return new BalanceWrapper(items);
    }

    private void buildModeRegelement(List<BalanceCard> items, List<Tuple> mvtCaissesByModes) {
        if (!CollectionUtils.isEmpty(mvtCaissesByModes)) {
            long total = mvtCaissesByModes.stream().mapToLong(t -> Long.parseLong(t.value() + "")).sum();
            items.add(new BalanceCard(CartName.MODE_PAIEMENT, total,
                mvtCaissesByModes.stream()
                    .map(tuple -> new Balance(tuple.libelle(), NumberUtil.formatToString(Long.parseLong(tuple.value() + "")), ((Long.parseLong(tuple.value() + "") * 100) / total) + ""))
                    .toList()));
        }
    }

    private void buildMvt(List<BalanceCard> items, List<Tuple> mvtCaisses) {
        if (!CollectionUtils.isEmpty(mvtCaisses)) {
            long total = mvtCaisses.stream().mapToLong(t -> Long.parseLong(t.value() + "")).sum();
            items.add(new BalanceCard(CartName.TYPE_MVT, total,
                mvtCaisses.stream()
                    .map(tuple -> new Balance(tuple.libelle(), NumberUtil.formatToString(Long.parseLong(tuple.value() + "")), ((Long.parseLong(tuple.value() + "") * 100) / total) + ""))
                    .toList()));
        }
    }

    private void buildSammary(BalanceCaisseWrapper balanceCaisseWrapper, List<BalanceCard> items) {

        List<Balance> balances = new ArrayList<>();
        balances.add(new Balance("Ttc", NumberUtil.formatToString(balanceCaisseWrapper.getMontantTtc()), null));
        balances.add(new Balance("Tva", NumberUtil.formatToString(balanceCaisseWrapper.getMontantTaxe()), null));
        balances.add(new Balance("Ht", NumberUtil.formatToString(balanceCaisseWrapper.getMontantHt()), null));
        if (balanceCaisseWrapper.getMontantDiscount() > 0) {
            balances.add(new Balance("Remise", NumberUtil.formatToString(balanceCaisseWrapper.getMontantDiscount()), null));
            balances.add(new Balance("net", NumberUtil.formatToString(balanceCaisseWrapper.getMontantNet()), null));
        }
        balances.add(new Balance("Ht", NumberUtil.formatToString(balanceCaisseWrapper.getMontantHt()), null));
        balances.add(new Balance("Comptant", NumberUtil.formatToString(balanceCaisseWrapper.getMontantPaye()), null));
        balances.add(new Balance("Crédit", NumberUtil.formatToString(balanceCaisseWrapper.getPartTiersPayant() + balanceCaisseWrapper.getMontantDiffere()), null));
        balances.add(new Balance("Panier moyen", NumberUtil.formatToString(balanceCaisseWrapper.getPanierMoyen()), null));
        balances.add(new Balance("Nbre client", NumberUtil.formatToString(balanceCaisseWrapper.getCount()), null));
        balances.add(new Balance("Valeur Achat", NumberUtil.formatToString(balanceCaisseWrapper.getMontantAchat()), null));
        balances.add(new Balance("Marge", NumberUtil.formatToString(balanceCaisseWrapper.getMontantMarge()), null));
        balances.add(new Balance("Ratio V/A", balanceCaisseWrapper.getRatioVenteAchat() + "", null));
        balances.add(new Balance("Ratio A/V", balanceCaisseWrapper.getRatioAchatVente() + "", null));
        items.add(new BalanceCard(CartName.RESUME, null, balances));
    }

    private void buildTypeVente(List<BalanceCard> items, List<BalanceCaisseDTO> balanceCaisses) {
        if (!CollectionUtils.isEmpty(balanceCaisses)) {
            items.add(new BalanceCard(CartName.TYPE_VENTE, balanceCaisses.stream().mapToLong(BalanceCaisseDTO::getMontantTtc).sum(),
                balanceCaisses.stream()
                    .map(bl -> new Balance(bl.getTypeVeTypeAffichage().getValue(), NumberUtil.formatToString(bl.getMontantTtc()), bl.getTypeSalePercent() + ""
                    ))
                    .toList()));
        }
    }
}


