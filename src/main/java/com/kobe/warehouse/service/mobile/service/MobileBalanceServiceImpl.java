package com.kobe.warehouse.service.mobile.service;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.service.dto.records.Tuple;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseService;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.mobile.dto.BalanceCard;
import com.kobe.warehouse.service.mobile.dto.BalanceWrapper;
import com.kobe.warehouse.service.mobile.dto.CartName;
import com.kobe.warehouse.service.mobile.dto.ListItem;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
            items.add(
                new BalanceCard(
                    CartName.MODE_PAIEMENT,
                    total,
                    mvtCaissesByModes
                        .stream()
                        .map(tuple ->
                            new ListItem(
                                tuple.libelle(),
                                NumberUtil.formatToString(Long.parseLong(tuple.value() + "")),
                                BigDecimal.valueOf(((Double.parseDouble(tuple.value() + "") * 100) / total))
                                    .round(new MathContext(2, RoundingMode.HALF_UP))
                                    .doubleValue() +
                                ""
                            )
                        )
                        .toList()
                )
            );
        }
    }

    private void buildMvt(List<BalanceCard> items, List<Tuple> mvtCaisses) {
        if (!CollectionUtils.isEmpty(mvtCaisses)) {
            long total = mvtCaisses.stream().mapToLong(t -> Long.parseLong(t.value() + "")).sum();
            items.add(
                new BalanceCard(
                    CartName.TYPE_MVT,
                    total,
                    mvtCaisses
                        .stream()
                        .map(tuple ->
                            new ListItem(
                                tuple.libelle(),
                                NumberUtil.formatToString(Long.parseLong(tuple.value() + "")),
                                BigDecimal.valueOf(((Double.parseDouble(tuple.value() + "") * 100) / total))
                                    .round(new MathContext(2, RoundingMode.HALF_UP))
                                    .doubleValue() +
                                ""
                            )
                        )
                        .toList()
                )
            );
        }
    }

    private void buildSammary(BalanceCaisseWrapper balanceCaisseWrapper, List<BalanceCard> items) {
        List<ListItem> balances = new ArrayList<>();
        balances.add(new ListItem("Ttc", NumberUtil.formatToString(balanceCaisseWrapper.getMontantTtc()), null));
        balances.add(new ListItem("Tva", NumberUtil.formatToString(balanceCaisseWrapper.getMontantTaxe()), null));
        balances.add(new ListItem("Ht", NumberUtil.formatToString(balanceCaisseWrapper.getMontantHt()), null));
        if (balanceCaisseWrapper.getMontantDiscount() > 0) {
            balances.add(new ListItem("Remise", NumberUtil.formatToString(balanceCaisseWrapper.getMontantDiscount()), null));
            balances.add(new ListItem("net", NumberUtil.formatToString(balanceCaisseWrapper.getMontantNet()), null));
        }
        balances.add(new ListItem("Ht", NumberUtil.formatToString(balanceCaisseWrapper.getMontantHt()), null));
        balances.add(new ListItem("Comptant", NumberUtil.formatToString(balanceCaisseWrapper.getMontantPaye()), null));
        balances.add(
            new ListItem(
                "Crédit",
                NumberUtil.formatToString(balanceCaisseWrapper.getPartTiersPayant() + balanceCaisseWrapper.getMontantDiffere()),
                null
            )
        );
        balances.add(new ListItem("Panier moyen", NumberUtil.formatToString(balanceCaisseWrapper.getPanierMoyen()), null));
        balances.add(new ListItem("Nbre client", NumberUtil.formatToString(balanceCaisseWrapper.getCount()), null));
        balances.add(new ListItem("Valeur Achat", NumberUtil.formatToString(balanceCaisseWrapper.getMontantAchat()), null));
        balances.add(new ListItem("Marge", NumberUtil.formatToString(balanceCaisseWrapper.getMontantMarge()), null));
        balances.add(new ListItem("Ratio V/A", balanceCaisseWrapper.getRatioVenteAchat() + "", null));
        balances.add(new ListItem("Ratio A/V", balanceCaisseWrapper.getRatioAchatVente() + "", null));
        items.add(new BalanceCard(CartName.RESUME, null, balances));
    }

    private void buildTypeVente(List<BalanceCard> items, List<BalanceCaisseDTO> balanceCaisses) {
        if (!CollectionUtils.isEmpty(balanceCaisses)) {
            items.add(
                new BalanceCard(
                    CartName.TYPE_VENTE,
                    balanceCaisses.stream().mapToLong(BalanceCaisseDTO::getMontantTtc).sum(),
                    balanceCaisses
                        .stream()
                        .map(bl ->
                            new ListItem(
                                bl.getTypeVeTypeAffichage().getValue(),
                                NumberUtil.formatToString(bl.getMontantTtc()),
                                bl.getTypeSalePercent() + ""
                            )
                        )
                        .toList()
                )
            );
        }
    }
}
