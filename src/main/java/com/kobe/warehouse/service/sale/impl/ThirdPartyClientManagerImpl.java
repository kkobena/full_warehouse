package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import com.kobe.warehouse.service.sale.ThirdPartyCalculationManager;
import com.kobe.warehouse.service.sale.ThirdPartyClientManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;

/**
 * Implémentation du service de gestion des clients tiers-payants.
 * Ce service gère toutes les opérations liées aux tiers-payants dans les ventes.
 */
@Service
@Transactional
public class ThirdPartyClientManagerImpl implements ThirdPartyClientManager {

    private final ThirdPartySaleLineService thirdPartySaleLineService;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final TiersPayantRepository tiersPayantRepository;
    private final ThirdPartySaleRepository thirdPartySaleRepository;
    private final ConsommationService consommationService;
    private final StorageService storageService;
    private final AssuranceItemIdGeneratorService assuranceItemIdGeneratorService;
    private final ThirdPartyCalculationManager calculationManager;

    public ThirdPartyClientManagerImpl(
        ThirdPartySaleLineService thirdPartySaleLineService,
        ClientTiersPayantRepository clientTiersPayantRepository,
        TiersPayantRepository tiersPayantRepository,
        ThirdPartySaleRepository thirdPartySaleRepository,
        ConsommationService consommationService,
        StorageService storageService,
        AssuranceItemIdGeneratorService assuranceItemIdGeneratorService,
        ThirdPartyCalculationManager calculationManager
    ) {
        this.thirdPartySaleLineService = thirdPartySaleLineService;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.tiersPayantRepository = tiersPayantRepository;
        this.thirdPartySaleRepository = thirdPartySaleRepository;
        this.consommationService = consommationService;
        this.storageService = storageService;
        this.assuranceItemIdGeneratorService = assuranceItemIdGeneratorService;
        this.calculationManager = calculationManager;
    }

    @Override
    public List<ClientTiersPayant> getClientTiersPayants(Set<Integer> ids) {
        return clientTiersPayantRepository.findAllByIdIn(ids);
    }

    @Override
    public String saveTiersPayantLines(ThirdPartySaleDTO dto, ThirdPartySales thirdPartySales)
        throws NumBonAlreadyUseException, GenericError {
        List<ClientTiersPayantDTO> tiersPayants = dto.getTiersPayants();
        if (CollectionUtils.isEmpty(tiersPayants)) {
            throw new GenericError("Aucun tiers payant n'a été fourni");
        }
        List<ClientTiersPayant> clientTiersPayants = getClientTiersPayants(
            tiersPayants.stream().map(ClientTiersPayantDTO::getId).collect(toSet())
        );
        List<CompteTiersPayant> compteTiersPayants = new ArrayList<>(clientTiersPayants.size());
        for (ClientTiersPayant clientTiersPayant : clientTiersPayants) {
            ClientTiersPayantDTO clientTiersPayantDTO = dto
                .getTiersPayants()
                .stream()
                .filter(ctpdto -> Objects.equals(ctpdto.getId(), clientTiersPayant.getId()))
                .findFirst()
                .orElseThrow(() -> new GenericError("Client tiers payant introuvable"));

            if (clientTiersPayantDTO.getNumBon() != null && checkIfNumBonIsAlReadyUse(clientTiersPayantDTO.getNumBon(), clientTiersPayant.getId(), null)) {
                throw new NumBonAlreadyUseException(clientTiersPayantDTO.getNumBon());
            }

            ThirdPartySaleLine thirdPartySaleLine = thirdPartySaleLineService.createThirdPartySaleLine(
                clientTiersPayantDTO.getNumBon(),
                clientTiersPayant,
                0
            );

            thirdPartySaleLine.setSale(thirdPartySales);
            short tauxVente = Objects.requireNonNullElse(clientTiersPayantDTO.getTaux(), 0).shortValue();
            thirdPartySaleLine.setTauxVente(tauxVente);
            thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
            compteTiersPayants.add(new CompteTiersPayant(clientTiersPayant, thirdPartySaleLine.getNumBon(), tauxVente));
        }
        return calculationManager.upddateThirdPartySaleAmounts(thirdPartySales, true, compteTiersPayants);
    }

    @Override
    public boolean checkIfNumBonIsAlReadyUse(String numBon, Integer clientTiersPayantId, Long currentSaleId) {
        if (!StringUtils.hasLength(numBon)) {
            return false;
        }
        if (isNull(currentSaleId)) {
            return thirdPartySaleLineService.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
                numBon,
                clientTiersPayantId,
                SalesStatut.CLOSED
            ) > 0;
        }
        return thirdPartySaleLineService.countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
            numBon,
            currentSaleId,
            clientTiersPayantId,
            SalesStatut.CLOSED
        ) > 0;
    }

    @Override
    public String addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, SaleId saleId)
        throws NumBonAlreadyUseException, GenericError {
        ClientTiersPayant clientTiersPayant = clientTiersPayantRepository.getReferenceById(dto.getId());
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.findOneById(saleId.getId());

        if (checkIfNumBonIsAlReadyUse(dto.getNumBon(), clientTiersPayant.getId(), null)) {
            throw new NumBonAlreadyUseException(dto.getNumBon());
        }

        ThirdPartySaleLine thirdPartySaleLine = thirdPartySaleLineService.createThirdPartySaleLine(
            dto.getNumBon(),
            clientTiersPayant,
            0
        );
        thirdPartySaleLine.setSale(thirdPartySales);
        short tauxVente = Objects.requireNonNullElse(dto.getTaux(), 0).shortValue();
        thirdPartySaleLine.setTauxVente(tauxVente);
        thirdPartySaleLineService.save(thirdPartySaleLine);
        thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);

        return calculationManager.reComputeAndApplyAmounts(thirdPartySales, null, true);
    }

    @Override
    public String removeThirdPartySaleLineToSales(Integer clientTiersPayantId, SaleId saleId) {
        Optional<ThirdPartySaleLine> saleLineOpt = thirdPartySaleLineService.findFirstByClientTiersPayantIdAndSaleId(
            clientTiersPayantId,
            saleId
        );

        if (saleLineOpt.isPresent()) {
            ThirdPartySaleLine thirdPartySaleLine = saleLineOpt.get();
            ThirdPartySales thirdPartySales = thirdPartySaleLine.getSale();
            thirdPartySaleLine.setSale(null);
            thirdPartySales.getThirdPartySaleLines().remove(thirdPartySaleLine);
            thirdPartySaleLineService.delete(thirdPartySaleLine);

            return calculationManager.reComputeAndApplyAmounts(thirdPartySales, null, true);
        }
        return null;
    }

    @Override
    public void updateClientTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine) {
        consommationService.updateConsommation(
            thirdPartySaleLine.getClientTiersPayant(),
            thirdPartySaleLine.getMontant(),
            thirdPartySaleLine.getUpdated(),
            clientTiersPayantRepository::save
        );
    }

    @Override
    public void updateTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine) {
        TiersPayant tiersPayant = thirdPartySaleLine.getClientTiersPayant().getTiersPayant();
        tiersPayant.setUser(storageService.getUser());
        consommationService.updateConsommation(
            tiersPayant,
            thirdPartySaleLine.getMontant(),
            thirdPartySaleLine.getUpdated(),
            tiersPayantRepository::save
        );
    }


    @Override
    public List<ThirdPartySaleLine> findAllBySaleId(SaleId saleId) {
        return thirdPartySaleLineService.findAllBySaleId(saleId);
    }

    @Override
    public ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy) {
        ThirdPartySaleLine clone = (ThirdPartySaleLine) original.clone();
        clone.setId(assuranceItemIdGeneratorService.nextId());
        clone.setSaleDate(LocalDate.now());
        clone.setStatut(ThirdPartySaleStatut.DELETE);
        clone.setMontant(clone.getMontant() * (-1));
        clone.setSale(copy);
        thirdPartySaleLineService.save(clone);
        original.setStatut(ThirdPartySaleStatut.DELETE);
        thirdPartySaleLineService.save(original);
        return clone;
    }

    @Override
    public List<ThirdPartySaleLine> clone(List<ThirdPartySaleLine> originals, ThirdPartySales copy) {
        if (CollectionUtils.isEmpty(originals)) {
            return new ArrayList<>();
        }
        List<ThirdPartySaleLine> clones = new ArrayList<>();
        for (ThirdPartySaleLine original : originals) {
            ThirdPartySaleLine clone = (ThirdPartySaleLine) original.clone();
            clone.setId(assuranceItemIdGeneratorService.nextId());
            clone.setSaleDate(copy.getSaleDate());
            clone.setSale(copy);
            clones.add(clone);

        }
        return clones;

    }

    @Override
    public void saveAll(List<ThirdPartySaleLine> thirdPartySaleLines) {
        if (!CollectionUtils.isEmpty(thirdPartySaleLines)) {
            thirdPartySaleLineService.saveAll(thirdPartySaleLines);
        }

    }

    @Override
    public void updateThirdPartySaleLine(
        String numBon,
        ThirdPartySaleLine thirdPartySaleLine,
        Integer clientTiersPayantId,
        Integer montant
    ) throws NumBonAlreadyUseException {
        ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();

        if (!clientTiersPayant.getId().equals(clientTiersPayantId)) {
            clientTiersPayant = clientTiersPayantRepository.getReferenceById(clientTiersPayantId);
            thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        }

        if (StringUtils.hasLength(numBon)) {
            SaleId saleId = thirdPartySaleLine.getSale().getId();
            if (checkIfNumBonIsAlReadyUse(numBon, clientTiersPayantId, saleId.getId())) {
                throw new NumBonAlreadyUseException(numBon);
            }
            thirdPartySaleLine.setNumBon(numBon);
        }

        if (montant != null) {
            thirdPartySaleLine.setMontant(montant);
        }

        thirdPartySaleLineService.save(thirdPartySaleLine);
    }

    @Override
    public Optional<ThirdPartySaleLine> findSaleLineByClientTiersPayantId(ThirdPartySales sale, Integer clientTiersPayantId) {
        return sale
            .getThirdPartySaleLines()
            .stream()
            .filter(line -> line.getClientTiersPayant().getId().equals(clientTiersPayantId))
            .findFirst();
    }

    @Override
    public String saveTiersPayantLinesOnChangeCustomer(ThirdPartySales thirdPartySales) {
        List<ClientTiersPayant> clientTiersPayants = ((AssuredCustomer) thirdPartySales.getCustomer())
            .getClientTiersPayants()
            .stream()
            .sorted(Comparator.comparingInt(c -> c.getPriorite().getValue()))
            .toList();

        for (ClientTiersPayant clientTiersPayant : clientTiersPayants) {
            ThirdPartySaleLine thirdPartySaleLine = thirdPartySaleLineService.createThirdPartySaleLine(
                null,
                clientTiersPayant,
                0
            );
            thirdPartySaleLine.setSale(thirdPartySales);
            thirdPartySaleLine.setTauxVente((short) clientTiersPayant.getTaux());
            thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
        }

        return calculationManager.reComputeAndApplyAmounts(thirdPartySales, buildCompteTiersPayantFromCompt(clientTiersPayants), true);
    }

    private List<CompteTiersPayant> buildCompteTiersPayantFromCompt(List<ClientTiersPayant> clientTiersPayants) {
        return clientTiersPayants
            .stream()
            .map(clientTiersPayant -> new CompteTiersPayant(clientTiersPayant, null, (short) clientTiersPayant.getTaux()))
            .toList();
    }
}
