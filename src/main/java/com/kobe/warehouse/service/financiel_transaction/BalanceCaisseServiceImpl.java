package com.kobe.warehouse.service.financiel_transaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.PaymentDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.DateUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class BalanceCaisseServiceImpl implements BalanceCaisseService {

    private static final Logger LOG = LoggerFactory.getLogger(BalanceCaisseServiceImpl.class);
    private final BalanceReportReportService balanceReportService;
    private final SalesRepository salesRepository;
    private final JsonMapper objectMapper;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AppConfigurationService appConfigurationService;

    public BalanceCaisseServiceImpl(
        BalanceReportReportService balanceReportService,
        SalesRepository salesRepository,
        PaymentTransactionRepository paymentTransactionRepository,
        AppConfigurationService appConfigurationService,
        JsonMapper objectMapper
    ) {
        this.balanceReportService = balanceReportService;
        this.salesRepository = salesRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.appConfigurationService = appConfigurationService;
        this.objectMapper = objectMapper;
    }

    public BalanceCaisseWrapper getBalanceCaisseNew(MvtParam mvtParam) {
        mvtParam.setStatuts(
            Set.of( SalesStatut.CLOSED,SalesStatut.CANCELED)
        );
        List<BalanceCaisseDTO> mvt = paymentTransactionRepository.fetchPaymentTransactionsForBalanceCaisse(mvtParam);
        BalanceCaisseWrapper balanceCaisseWrapper = computeBalanceCaisses(fetchBalanceCaisse(mvtParam));
        updateModePayment(balanceCaisseWrapper, mvt);
        computeMvts(balanceCaisseWrapper, mvt);
        balanceCaisseWrapper.setPeriode("Du " + DateUtil.format(mvtParam.getFromDate()) + " au " + DateUtil.format(mvtParam.getToDate()));
        return balanceCaisseWrapper;
    }

    private List<BalanceCaisseDTO> fetchBalanceCaisse(MvtParam mvtParam) {
        try {
            String jsonResult = salesRepository.fetchSalesBalance(
                mvtParam.getFromDate(),
                mvtParam.getToDate(),
                mvtParam.getStatuts().stream().map(SalesStatut::name).toArray(String[]::new),
                mvtParam.getCategorieChiffreAffaires().stream().map(CategorieChiffreAffaire::name).toArray(String[]::new),
                mvtParam.isExcludeFreeUnit(),
                BooleanUtils.toBoolean(mvtParam.getToIgnore())
            );

            return objectMapper.readValue(jsonResult, new TypeReference<>() {});
        } catch (Exception e) {
            LOG.error(null, e);
            return List.of();
        }
    }

    @Override
    public BalanceCaisseWrapper getBalanceCaisse(MvtParam mvtParam) {
        mvtParam.setExcludeFreeUnit(appConfigurationService.excludeFreeUnit());
        return getBalanceCaisseNew(mvtParam);
    }

    private BalanceCaisseWrapper computeBalanceCaisses(List<BalanceCaisseDTO> balanceCaisseDTOS) {
        BalanceCaisseWrapper balanceCaisseWrapper = new BalanceCaisseWrapper();
        BalanceCaisseDTO vno = null;
        BalanceCaisseDTO vo = null;
        List<com.kobe.warehouse.service.dto.records.Tuple> mvtCaissesByModes = new ArrayList<>();
        List<BalanceCaisseDTO> balanceCaisses = new ArrayList<>();
        for (Entry<TypeVenteDTO, List<BalanceCaisseDTO>> typeVenteListEntry : balanceCaisseDTOS
            .stream()
            .collect(Collectors.groupingBy(BalanceCaisseDTO::getTypeSale))
            .entrySet()) {
            switch (typeVenteListEntry.getKey()) {
                case CashSale:
                    if (vno == null) {
                        vno = new BalanceCaisseDTO();
                    }
                    vno.setTypeVeTypeAffichage(TransactionTypeAffichage.VNO);
                    vno.setTypeSale(TypeVenteDTO.CashSale);
                    upadateBalance(vno, typeVenteListEntry.getValue(), mvtCaissesByModes);
                    break;
                case ThirdPartySales, VenteDepotAgree:
                    if (vo == null) {
                        vo = new BalanceCaisseDTO();
                    }
                    vo.setTypeVeTypeAffichage(TransactionTypeAffichage.VO);
                    vo.setTypeSale(TypeVenteDTO.ThirdPartySales);
                    upadateBalance(vo, typeVenteListEntry.getValue(), mvtCaissesByModes);
                    break;
                case VenteDepot:
                    balanceCaisseWrapper.setMontantDepot(
                        typeVenteListEntry.getValue().stream().mapToLong(BalanceCaisseDTO::getMontantTtc).sum()
                    );
                    break;
            }
        }
        updateBalanceCaisseWrapper(balanceCaisseWrapper, vo);
        updateBalanceCaisseWrapper(balanceCaisseWrapper, vno);
        if (vno != null) {
            computePercent(vno, balanceCaisseWrapper);
            balanceCaisses.add(vno);
        }
        if (vo != null) {
            computePercent(vo, balanceCaisseWrapper);
            balanceCaisses.add(vo);
        }

        if (balanceCaisseWrapper.getCount() > 0) {
            balanceCaisseWrapper.setPanierMoyen(balanceCaisseWrapper.getMontantTtc() / balanceCaisseWrapper.getCount());
        }
        computeRatioVenteAchat(balanceCaisseWrapper);
        computeRatioAchatVente(balanceCaisseWrapper);
        balanceCaisseWrapper.setMvtCaissesByModes(mvtCaissesByModes);
        balanceCaisseWrapper.setBalanceCaisses(balanceCaisses);
        return balanceCaisseWrapper;
    }

    private void computeRatioVenteAchat(BalanceCaisseWrapper balanceCaisseWrapper) {
        if (balanceCaisseWrapper.getMontantAchat() == 0L) {
            return;
        }
        balanceCaisseWrapper.setRatioVenteAchat(
            BigDecimal.valueOf((double) balanceCaisseWrapper.getMontantTtc() / balanceCaisseWrapper.getMontantAchat())
                .setScale(2, RoundingMode.HALF_UP)
                .floatValue()
        );
    }

    private void computeRatioAchatVente(BalanceCaisseWrapper balanceCaisseWrapper) {
        if (balanceCaisseWrapper.getMontantTtc() == 0L) {
            return;
        }
        balanceCaisseWrapper.setRatioAchatVente(
            BigDecimal.valueOf((double) balanceCaisseWrapper.getMontantAchat() / balanceCaisseWrapper.getMontantTtc())
                .setScale(2, RoundingMode.HALF_UP)
                .floatValue()
        );
    }

    private void computePercent(BalanceCaisseDTO balanceCaisse, BalanceCaisseWrapper balanceCaisseWrapper) {
        var pourcentage = (short) Math.round(
            ((double) balanceCaisse.getMontantNet() * 100) / Math.abs(balanceCaisseWrapper.getMontantNet())
        );
        balanceCaisse.setTypeSalePercent(pourcentage);
    }

    private void updateBalanceCaisseWrapper(BalanceCaisseWrapper balanceCaisseWrapper, BalanceCaisseDTO b) {
        if (Objects.isNull(b)) {
            return;
        }
        balanceCaisseWrapper.setCount(balanceCaisseWrapper.getCount() + b.getCount());
        balanceCaisseWrapper.setMontantDiscount(balanceCaisseWrapper.getMontantDiscount() + b.getMontantDiscount());
        balanceCaisseWrapper.setMontantTtc(balanceCaisseWrapper.getMontantTtc() + b.getMontantTtc());
        balanceCaisseWrapper.setMontantPaye(balanceCaisseWrapper.getMontantPaye() + b.getMontantPaye());
        balanceCaisseWrapper.setMontantHt(balanceCaisseWrapper.getMontantHt() + b.getMontantHt());
        balanceCaisseWrapper.setMontantNet(balanceCaisseWrapper.getMontantNet() + b.getMontantNet());
        balanceCaisseWrapper.setMontantAchat(balanceCaisseWrapper.getMontantAchat() + b.getMontantAchat());
        balanceCaisseWrapper.setMontantMarge(balanceCaisseWrapper.getMontantMarge() + b.getMontantMarge());
        balanceCaisseWrapper.setAmountToBePaid(balanceCaisseWrapper.getAmountToBePaid() + b.getAmountToBePaid());
        balanceCaisseWrapper.setAmountToBeTakenIntoAccount(
            balanceCaisseWrapper.getAmountToBeTakenIntoAccount() + b.getAmountToBeTakenIntoAccount()
        );
        balanceCaisseWrapper.setMontantNetUg(balanceCaisseWrapper.getMontantNetUg() + b.getMontantNetUg());
        balanceCaisseWrapper.setMontantTtcUg(balanceCaisseWrapper.getMontantTtcUg() + b.getMontantTtcUg());
        balanceCaisseWrapper.setMontantHtUg(balanceCaisseWrapper.getMontantHtUg() + b.getMontantHtUg());
        balanceCaisseWrapper.setPartAssure(balanceCaisseWrapper.getPartAssure() + b.getPartAssure());
        balanceCaisseWrapper.setMontantTaxe(balanceCaisseWrapper.getMontantTaxe() + b.getMontantTaxe());
        balanceCaisseWrapper.setPartTiersPayant(balanceCaisseWrapper.getPartTiersPayant() + b.getPartTiersPayant());
        balanceCaisseWrapper.setMontantCash(balanceCaisseWrapper.getMontantCash() + b.getMontantCash());
        balanceCaisseWrapper.setMontantMobileMoney(balanceCaisseWrapper.getMontantMobileMoney() + b.getMontantMobileMoney());
        balanceCaisseWrapper.setMontantCard(balanceCaisseWrapper.getMontantCard() + b.getMontantCard());
        balanceCaisseWrapper.setMontantVirement(balanceCaisseWrapper.getMontantVirement() + b.getMontantVirement());
        balanceCaisseWrapper.setMontantCheck(balanceCaisseWrapper.getMontantCheck() + b.getMontantCheck());
        balanceCaisseWrapper.setMontantDiffere(balanceCaisseWrapper.getMontantDiffere() + b.getMontantDiffere());
    }

    private void upadateBalance(
        BalanceCaisseDTO b,
        List<BalanceCaisseDTO> balanceCaisses,
        List<com.kobe.warehouse.service.dto.records.Tuple> mvtCaissesByModes
    ) {
        for (BalanceCaisseDTO e : balanceCaisses) {
            List<PaymentDTO> payments = e.getPayments();
            for (PaymentDTO p : payments) {
                b.setMontantPaye(b.getMontantPaye() + p.paidAmount());
                b.setMontantReel(b.getMontantReel() + p.realAmount());

                ModePaimentCode modePaimentCode = ModePaimentCode.fromName(p.code());
                if (Objects.nonNull(modePaimentCode)) {
                    mvtCaissesByModes.add(new com.kobe.warehouse.service.dto.records.Tuple(p.code(), p.libelle(), p.paidAmount()));
                    switch (modePaimentCode) {
                        case CASH:
                            b.setMontantCash(b.getMontantCash() + p.paidAmount());
                            break;
                        case OM, MTN, MOOV, WAVE:
                            b.setMontantMobileMoney(b.getMontantMobileMoney() + p.paidAmount());
                            break;
                        case CB:
                            b.setMontantCard(b.getMontantCard() + p.paidAmount());
                            break;
                        case VIREMENT:
                            b.setMontantVirement(b.getMontantVirement() + p.paidAmount());
                            break;
                        case CH:
                            b.setMontantCheck(b.getMontantCheck() + p.paidAmount());
                            break;
                    }
                }
            }

            b.setCount(b.getCount() + e.getCount());
            b.setMontantDiscount(b.getMontantDiscount() + e.getMontantDiscount());
            b.setMontantTtc(b.getMontantTtc() + e.getMontantTtc());
            b.setMontantHt(b.getMontantHt() + e.getMontantHt());
            b.setMontantNet(b.getMontantNet() + e.getMontantNet());
            b.setMontantAchat(b.getMontantAchat() + e.getMontantAchat());
            b.setMontantNetUg(b.getMontantNetUg() + e.getMontantNetUg());
            b.setMontantTtcUg(b.getMontantTtcUg() + e.getMontantTtcUg());
            b.setMontantHtUg(b.getMontantHtUg() + e.getMontantHtUg());
            b.setPartAssure(b.getPartAssure() + e.getPartAssure());
            b.setPartTiersPayant(b.getPartTiersPayant() + e.getPartTiersPayant());
        }
    }

    private void computeMvts(BalanceCaisseWrapper balanceCaisseWrapper, List<BalanceCaisseDTO> balanceCaisses) {
        balanceCaisses
            .stream()
            .collect(Collectors.groupingBy(BalanceCaisseDTO::getTypeVeTypeAffichage))
            .forEach((k, v) -> {
                long amount = v.stream().mapToLong(BalanceCaisseDTO::getMontantPaye).sum();
                balanceCaisseWrapper.getMvtCaisses().add(new com.kobe.warehouse.service.dto.records.Tuple(k.name(), k.getValue(), amount));
            });
    }

    private List<com.kobe.warehouse.service.dto.records.Tuple> computeMvtModesPaiment(List<BalanceCaisseDTO> balanceCaisses) {
        List<com.kobe.warehouse.service.dto.records.Tuple> mvtCaissesByModes = new ArrayList<>();
        balanceCaisses
            .stream()
            .collect(Collectors.groupingBy(BalanceCaisseDTO::getModePaiement))
            .forEach((k, v) -> {
                long amount = v.stream().mapToLong(BalanceCaisseDTO::getMontantPaye).sum();
                mvtCaissesByModes.add(new com.kobe.warehouse.service.dto.records.Tuple(k, v.getFirst().getLibelleModePaiement(), amount));
            });
        return mvtCaissesByModes;
    }

    private void updateModePayment(BalanceCaisseWrapper balanceCaisseWrapper, List<BalanceCaisseDTO> balanceCaisses) {
        List<com.kobe.warehouse.service.dto.records.Tuple> mvtCaissesByModes = balanceCaisseWrapper.getMvtCaissesByModes();
        mvtCaissesByModes.addAll(computeMvtModesPaiment(balanceCaisses));
        balanceCaisseWrapper.setMvtCaissesByModes(new ArrayList<>());
        mvtCaissesByModes
            .stream()
            .collect(Collectors.groupingBy(com.kobe.warehouse.service.dto.records.Tuple::key))
            .forEach((k, v) -> {
                long amount = v.stream().mapToLong(e -> Long.parseLong(e.value().toString())).sum();
                balanceCaisseWrapper
                    .getMvtCaissesByModes()
                    .add(new com.kobe.warehouse.service.dto.records.Tuple(k, v.getFirst().libelle(), amount));
            });
    }

    @Override
    public Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException {
        return this.balanceReportService.exportToPdf(
                getBalanceCaisse(mvtParam),
                new ReportPeriode(mvtParam.getFromDate(), mvtParam.getToDate())
            );
    }
}
