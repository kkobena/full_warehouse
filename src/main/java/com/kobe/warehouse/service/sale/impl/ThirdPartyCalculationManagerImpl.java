package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RepartitionTiersPayantParTva;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.service.produit_prix.service.PrixRererenceService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.ThirdPartyCalculationManager;
import com.kobe.warehouse.service.sale.calculation.TiersPayantCalculationService;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationInput;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationResult;
import com.kobe.warehouse.service.sale.calculation.dto.SaleItemInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantLineOutput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantPrixInput;
import com.kobe.warehouse.service.sale.calculation.dto.TvaRepartitionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;

/**
 * Implémentation du service de calcul des montants pour les ventes avec tiers-payants.
 * Ce service encapsule toute la logique complexe de répartition des montants.
 */
@Service
@Transactional
public class ThirdPartyCalculationManagerImpl implements ThirdPartyCalculationManager {

    private final TiersPayantCalculationService tiersPayantCalculationService;
    private final ThirdPartySaleLineService thirdPartySaleLineService;
    private final SalesLineService salesLineService;
    private final ThirdPartySaleRepository thirdPartySaleRepository;
    private final PrixRererenceService prixRererenceService;
    private final SaleCommonService saleCommonService;

    public ThirdPartyCalculationManagerImpl(
        TiersPayantCalculationService tiersPayantCalculationService,
        ThirdPartySaleLineService thirdPartySaleLineService,
        SaleLineServiceFactory saleLineServiceFactory,
        ThirdPartySaleRepository thirdPartySaleRepository,
        PrixRererenceService prixRererenceService,
        SaleCommonService saleCommonService
    ) {
        this.tiersPayantCalculationService = tiersPayantCalculationService;
        this.thirdPartySaleLineService = thirdPartySaleLineService;
        this.salesLineService = saleLineServiceFactory.getService(TypeVente.ThirdPartySales);
        this.thirdPartySaleRepository = thirdPartySaleRepository;
        this.prixRererenceService = prixRererenceService;
        this.saleCommonService = saleCommonService;
    }

    @Override
    public String upddateThirdPartySaleAmounts(
        ThirdPartySales thirdPartySales,
        boolean isUpdate,
        List<CompteTiersPayant> clientTiersPayants
    ) {
        saleCommonService.updateAmounts(thirdPartySales);
        return reComputeAndApplyAmounts(thirdPartySales, clientTiersPayants, isUpdate);
    }

    @Override
    public String reComputeAndApplyAmounts(
        ThirdPartySales thirdPartySales,
        List<CompteTiersPayant> clientTiersPayants,
        boolean isUpdate
    ) {
        if (CollectionUtils.isEmpty(clientTiersPayants)) {
            clientTiersPayants = thirdPartySales
                .getThirdPartySaleLines()
                .stream()
                .map(tp -> new CompteTiersPayant(tp.getClientTiersPayant(), tp.getNumBon(),
                    tp.getTauxVente()))
                .toList();
        }

        CalculationResult output = tiersPayantCalculationService.calculate(
            buildCalculationInput(thirdPartySales, clientTiersPayants)
        );
        if (isNull(output)) {
            thirdPartySales.setPartTiersPayant(0);
            thirdPartySales.setPartAssure(0);
            thirdPartySales.setAmountToBePaid(0);
            return null;
        }

        int totalPatientShare = output.getTotalPatientShare().intValue();
        thirdPartySales.setPartTiersPayant(output.getTotalTiersPayant().intValue());
        thirdPartySales.setPartAssure(totalPatientShare);
        thirdPartySales.setAmountToBePaid(saleCommonService.roundedAmount(totalPatientShare));

        // Apply results to ThirdPartySaleLine entities
        for (TiersPayantLineOutput lineResult : output.getTiersPayantLines()) {
            findSaleLineByClientTiersPayantId(thirdPartySales, lineResult.getClientTiersPayantId()).ifPresent(saleLine -> {
                saleLine.setMontant(lineResult.getMontant().intValue());
                saleLine.setTaux((short) lineResult.getFinalTaux());

                // Save TVA repartitions
                if (!CollectionUtils.isEmpty(lineResult.getRepartitions())) {
                    List<RepartitionTiersPayantParTva> repartitions = lineResult
                        .getRepartitions()
                        .stream()
                        .map(TvaRepartitionDto::toDomainRecord)
                        .toList();
                    saleLine.setRepartitions(repartitions);
                }

                if (isUpdate) {
                    this.thirdPartySaleLineService.save(saleLine);
                }
            });
        }

        // Apply item-level results to SalesLine entities
        for (SalesLine saleLine : thirdPartySales.getSalesLines()) {
            output
                .getItemShares()
                .stream()
                .filter(s -> s.getSaleLineId().equals(saleLine.getId().getId()))
                .findFirst()
                .ifPresent(itemShare -> {
                    saleLine.setCalculationBasePrice(itemShare.getCalculationBasePrice());
                    saleLine.setRates(itemShare.getRates());
                    if (isUpdate) {
                        this.salesLineService.saveSalesLine(saleLine);
                    }
                });
        }

        thirdPartySales.setHasPriceOption(output.hasPriceOption());
        if (isUpdate) {
            this.thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        }

        return output.getWarningMessage();
    }

    @Override
    public String computeThirdPartySaleAmounts(ThirdPartySales thirdPartySales) {
        saleCommonService.computeSaleEagerAmount(thirdPartySales);
        return upddateThirdPartySaleAmounts(thirdPartySales, true, null);
    }

    @Override
    public void upddateSaleAmountsOnRemovingItem(ThirdPartySales thirdPartySales) {
        saleCommonService.computeSaleEagerAmount(thirdPartySales);
        reComputeAndApplyAmounts(thirdPartySales, null, true);
    }

    /**
     * Construit l'input de calcul pour le service de calcul des tiers-payants.
     *
     * @param sale               la vente
     * @param clientTiersPayants la liste des clients tiers-payants
     * @return l'input de calcul
     */
    private CalculationInput buildCalculationInput(ThirdPartySales sale, List<CompteTiersPayant> clientTiersPayants) {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(sale.getNatureVente());
        input.setDiscountAmount(BigDecimal.valueOf(sale.getDiscountAmount()));

        Set<Integer> tiersPayantIds = new HashSet<>();
        List<TiersPayantInput> tiersPayantInputs = buildTiersPayantInputs(clientTiersPayants, tiersPayantIds);
        input.setTiersPayants(tiersPayantInputs);

        List<SaleItemInput> saleItemInputs = buildSaleItemInputs(sale, input, tiersPayantIds, tiersPayantInputs);
        input.setSaleItems(saleItemInputs);

        return input;
    }

    /**
     * Construit les inputs des tiers-payants.
     *
     * @param clientTiersPayants la liste des clients tiers-payants
     * @param tiersPayantIds     l'ensemble des identifiants de tiers-payants (modifié par effet de bord)
     * @return la liste des inputs de tiers-payants
     */
    private List<TiersPayantInput> buildTiersPayantInputs(List<CompteTiersPayant> clientTiersPayants, Set<Integer> tiersPayantIds) {
        if (CollectionUtils.isEmpty(clientTiersPayants)) {
            return Collections.emptyList();
        }
        return clientTiersPayants
            .stream()
            .map(ctp -> {
                ClientTiersPayant clientTiersPayant = ctp.clientTiersPayant();
                TiersPayantInput ti = new TiersPayantInput();
                TiersPayant tiersPayant = clientTiersPayant.getTiersPayant();
                tiersPayantIds.add(tiersPayant.getId());
                ti.setClientTiersPayantId(clientTiersPayant.getId());
                ti.setTiersPayantId(tiersPayant.getId());
                ti.setTiersPayantFullName(tiersPayant.getFullName());
                short cmpTaux = (short) clientTiersPayant.getTaux();
                if (ctp.tauxVente() > 0 && ctp.tauxVente() != cmpTaux) {
                    cmpTaux = ctp.tauxVente();
                }
                ti.setTaux(cmpTaux / 100.0f);
                ti.setPriorite(clientTiersPayant.getPriorite());
                Optional.ofNullable(tiersPayant.getPlafondConso()).ifPresent(v -> ti.setPlafondConso(BigDecimal.valueOf(v)));
                Optional.ofNullable(clientTiersPayant.getConsoMensuelle()).ifPresent(v -> ti.setConsoMensuelle(BigDecimal.valueOf(v)));
                Optional
                    .ofNullable(tiersPayant.getPlafondJournalierClient())
                    .ifPresent(v -> ti.setPlafondJournalierClient(BigDecimal.valueOf(v)));
                return ti;
            })
            .toList();
    }

    /**
     * Construit les inputs des articles de vente avec les taux de TVA.
     *
     * @param sale              la vente
     * @param input             l'input de calcul (modifié par effet de bord pour totalSalesAmount)
     * @param tiersPayantIds    l'ensemble des identifiants de tiers-payants
     * @param tiersPayantInputs la liste des inputs de tiers-payants
     * @return la liste des inputs d'articles
     */
    private List<SaleItemInput> buildSaleItemInputs(
        ThirdPartySales sale,
        CalculationInput input,
        Set<Integer> tiersPayantIds,
        List<TiersPayantInput> tiersPayantInputs
    ) {
        return sale
            .getSalesLines()
            .stream()
            .map(sl -> {
                SaleItemInput si = new SaleItemInput();
                Produit produit = sl.getProduit();
                si.setSalesLineId(sl.getId().getId());
                si.setTotalSalesAmount(BigDecimal.valueOf(sl.getSalesAmount()));
                si.setQuantity(sl.getQuantityRequested());
                si.setRegularUnitPrice(BigDecimal.valueOf(sl.getRegularUnitPrice()));

                // Extract TVA rate from product
                Tva tva = produit.getTva();
                si.setTvaRate(tva.getTaux());

                input.setTotalSalesAmount(
                    Objects.requireNonNullElse(input.getTotalSalesAmount(), BigDecimal.ZERO).add(si.getTotalSalesAmount())
                );

                // Add prix references for this product
                this.prixRererenceService.findByProduitIdAndTiersPayantIds(produit.getId(), tiersPayantIds).forEach(prixRef ->
                    tiersPayantInputs.forEach(cl -> {
                        if (cl.getTiersPayantId().compareTo(prixRef.getTiersPayant().getId()) == 0) {
                            TiersPayantPrixInput pi = new TiersPayantPrixInput();
                            pi.setCompteTiersPayantId(cl.getClientTiersPayantId());
                            pi.setPrice(prixRef.getPrice());
                            pi.setRate(prixRef.getRate());
                            pi.setOptionPrixType(prixRef.getType());
                            si.getPrixAssurances().add(pi);
                        }
                    })
                );
                return si;
            })
            .toList();
    }

    /**
     * Trouve une ligne de tiers-payant par l'identifiant du client tiers-payant.
     *
     * @param sale                la vente
     * @param clientTiersPayantId l'identifiant du client tiers-payant
     * @return Optional contenant la ligne si trouvée
     */
    private Optional<ThirdPartySaleLine> findSaleLineByClientTiersPayantId(ThirdPartySales sale, Integer clientTiersPayantId) {
        return sale
            .getThirdPartySaleLines()
            .stream()
            .filter(line -> line.getClientTiersPayant().getId().equals(clientTiersPayantId))
            .findFirst();
    }
}
