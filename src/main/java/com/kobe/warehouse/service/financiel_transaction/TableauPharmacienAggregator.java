package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.dto.projection.ReponseRetourBonItemProjection;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kobe.warehouse.service.financiel_transaction.TableauPharmacienConstants.GROUP_OTHER_ID;

/**
 * Handles data aggregation for TableauPharmacien
 */
@Component
public class TableauPharmacienAggregator {

    private final TableauPharmacienCalculator calculator;

    public TableauPharmacienAggregator(TableauPharmacienCalculator calculator) {
        this.calculator = calculator;
    }

    /**
     * Aggregate FournisseurAchat by group ID
     */
    public Map<Integer, Long> aggregateFournisseurAchatsByGroup(List<FournisseurAchat> groupAchats) {
        if (groupAchats == null || groupAchats.isEmpty()) {
            return Collections.emptyMap();
        }

        return groupAchats.stream()
            .collect(Collectors.groupingBy(
                FournisseurAchat::getId,
                Collectors.summingLong(f -> f.getAchat().getMontantNet())
            ));
    }

    /**
     * Build FournisseurAchat list grouped by supplier group for a specific day
     */
    public List<FournisseurAchat> buildFournisseurAchatsForDay(
        List<AchatDTO> achats,
        Set<Integer> displayedGroupIds
    ) {
        if (achats == null || achats.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, FournisseurAchat> fournisseurAchatMap = new HashMap<>();

        Map<Integer, List<AchatDTO>> groupedBySupplier = achats.stream()
            .collect(Collectors.groupingBy(AchatDTO::getGroupeGrossisteId));

        for (Map.Entry<Integer, List<AchatDTO>> entry : groupedBySupplier.entrySet()) {
            Integer groupId = entry.getKey();
            List<AchatDTO> groupAchats = entry.getValue();

            // Determine if this group should be displayed individually or grouped as "Others"
            Integer displayGroupId = displayedGroupIds.contains(groupId) ? groupId : GROUP_OTHER_ID;

            FournisseurAchat fournisseurAchat = fournisseurAchatMap.computeIfAbsent(
                displayGroupId,
                k -> createNewFournisseurAchat(k, groupAchats.getFirst().getGroupeGrossiste())
            );

            // Aggregate purchase amounts
            AchatDTO aggregatedAchat = calculator.aggregateAchats(groupAchats, fournisseurAchat.getAchat());
            fournisseurAchat.setAchat(aggregatedAchat);
        }

        return new ArrayList<>(fournisseurAchatMap.values());
    }

    /**
     * Aggregate all FournisseurAchat across multiple days
     */
    public List<FournisseurAchat> aggregateFournisseurAchatsAcrossDays(
        List<TableauPharmacienDTO> tableauPharmaciens
    ) {
        if (tableauPharmaciens == null || tableauPharmaciens.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, FournisseurAchat> aggregatedMap = new HashMap<>();

        tableauPharmaciens.stream()
            .flatMap(t -> t.getGroupAchats().stream())
            .forEach(fournisseurAchat -> {
                Integer groupId = fournisseurAchat.getId();
                FournisseurAchat existing = aggregatedMap.get(groupId);

                if (existing == null) {
                    FournisseurAchat newFournisseur = createNewFournisseurAchat(
                        groupId,
                        fournisseurAchat.getLibelle()
                    );
                    newFournisseur.setAchat(calculator.aggregateAchats(
                        List.of(fournisseurAchat.getAchat()),
                        newFournisseur.getAchat()
                    ));
                    aggregatedMap.put(groupId, newFournisseur);
                } else {
                    calculator.aggregateAchats(
                        List.of(fournisseurAchat.getAchat()),
                        existing.getAchat()
                    );
                }
            });

        return new ArrayList<>(aggregatedMap.values());
    }

    /**
     * Merge achats into TableauPharmacien entries by date
     */
    public void mergeAchatsIntoTableau(
        List<TableauPharmacienDTO> tableauPharmaciens,
        Map<LocalDate, List<AchatDTO>> achatsByDate,
        Set<Integer> displayedGroupIds
    ) {
        for (TableauPharmacienDTO dto : tableauPharmaciens) {
            List<AchatDTO> achatsForDate = achatsByDate.remove(dto.getMvtDate());

            if (achatsForDate != null && !achatsForDate.isEmpty()) {
                List<FournisseurAchat> groupAchats = buildFournisseurAchatsForDay(
                    achatsForDate,
                    displayedGroupIds
                );
                dto.setGroupAchats(groupAchats);
                dto.setMontantBonAchat(
                    achatsForDate.stream()
                        .mapToLong(AchatDTO::getMontantNet)
                        .sum()
                );
                dto.setAchatFournisseurs(aggregateFournisseurAchatsByGroup(groupAchats));
            }

            calculator.calculateRatioVenteAchat(dto);
            calculator.calculateRatioAchatVente(dto);
        }
    }

    /**
     * Create entries for dates that have achats but no sales
     */
    public List<TableauPharmacienDTO> createEntriesForAchatsOnly(
        Map<LocalDate, List<AchatDTO>> remainingAchats,
        Set<Integer> displayedGroupIds
    ) {
        List<TableauPharmacienDTO> newEntries = new ArrayList<>();

        remainingAchats.forEach((date, achats) -> {
            TableauPharmacienDTO dto = new TableauPharmacienDTO();
            dto.setMvtDate(date);

            List<FournisseurAchat> groupAchats = buildFournisseurAchatsForDay(achats, displayedGroupIds);
            dto.setGroupAchats(groupAchats);
            dto.setMontantBonAchat(
                achats.stream().mapToLong(AchatDTO::getMontantNet).sum()
            );
            dto.setAchatFournisseurs(aggregateFournisseurAchatsByGroup(groupAchats));

            newEntries.add(dto);
        });

        return newEntries;
    }

    /**
     * Aggregate wrapper totals from sales data
     */
    public void aggregateSalesToWrapper(
        TableauPharmacienWrapper wrapper,
        TableauPharmacienDTO dto
    ) {
        wrapper.setMontantVenteCredit(wrapper.getMontantVenteCredit() + dto.getMontantCredit());
        wrapper.setMontantVenteComptant(wrapper.getMontantVenteComptant() + dto.getMontantComptant());
        wrapper.setMontantVenteHt(wrapper.getMontantVenteHt() + dto.getMontantHt());
        wrapper.setMontantVenteTtc(wrapper.getMontantVenteTtc() + dto.getMontantTtc());
        wrapper.setMontantVenteTaxe(wrapper.getMontantVenteTaxe() + dto.getMontantTaxe());
        wrapper.setMontantVenteRemise(wrapper.getMontantVenteRemise() + dto.getMontantRemise());
        wrapper.setMontantVenteNet(wrapper.getMontantVenteNet() + dto.getMontantNet());
        wrapper.setNumberCount(wrapper.getNumberCount() + dto.getNombreVente());
    }

    /**
     * Aggregate wrapper totals from purchase data
     */
    public void aggregatePurchasesToWrapper(
        TableauPharmacienWrapper wrapper,
        AchatDTO achat
    ) {
        wrapper.setMontantAchatTtc(wrapper.getMontantAchatTtc() + achat.getMontantTtc());
        wrapper.setMontantAchatRemise(wrapper.getMontantAchatRemise() + achat.getMontantRemise());
        wrapper.setMontantAchatNet(wrapper.getMontantAchatNet() + achat.getMontantNet());
        wrapper.setMontantAchatTaxe(wrapper.getMontantAchatTaxe() + achat.getMontantTaxe());
        wrapper.setMontantAchatHt(wrapper.getMontantAchatHt() + achat.getMontantHt());
    }

    /**
     * Aggregate supplier returns (avoirs) and distribute by date
     * Returns list of dates that had avoirs but no existing tableau entries
     */
    public Map<LocalDate, Long> mergeSupplierReturnsIntoTableau(
        List<TableauPharmacienDTO> tableauPharmaciens,
        List<ReponseRetourBonItemProjection> avoirs
    ) {
        if (avoirs == null || avoirs.isEmpty()) {
            return Collections.emptyMap();
        }

        // Group avoirs by date
        Map<LocalDate, Long> avoirsByDate = avoirs.stream()
            .collect(Collectors.groupingBy(
                ReponseRetourBonItemProjection::getDateMtv,
                Collectors.summingLong(a -> a.getValeurAchat() != null ? a.getValeurAchat() : 0L)
            ));

        if (tableauPharmaciens == null || tableauPharmaciens.isEmpty()) {
            // Return all avoirs as unmatched
            return avoirsByDate;
        }

        // Merge avoirs into each TableauPharmacienDTO by date
        for (TableauPharmacienDTO dto : tableauPharmaciens) {
            Long avoirAmount = avoirsByDate.remove(dto.getMvtDate());
            if (avoirAmount != null) {
                dto.setMontantAvoirFournisseur(avoirAmount);
            }
        }

        // Return remaining avoirs that didn't match any date
        return avoirsByDate;
    }

    /**
     * Create entries for dates that have avoirs but no sales or purchases
     */
    public List<TableauPharmacienDTO> createEntriesForAvoirsOnly(Map<LocalDate, Long> unmatchedAvoirs) {
        if (unmatchedAvoirs == null || unmatchedAvoirs.isEmpty()) {
            return Collections.emptyList();
        }

        List<TableauPharmacienDTO> newEntries = new ArrayList<>();

        unmatchedAvoirs.forEach((date, avoirAmount) -> {
            TableauPharmacienDTO dto = new TableauPharmacienDTO();
            dto.setMvtDate(date);
            dto.setMontantAvoirFournisseur(avoirAmount);
            newEntries.add(dto);
        });

        return newEntries;
    }

    /**
     * Calculate total supplier returns from TableauPharmacien entries
     */
    public long calculateTotalSupplierReturns(List<TableauPharmacienDTO> tableauPharmaciens) {
        if (tableauPharmaciens == null || tableauPharmaciens.isEmpty()) {
            return 0L;
        }

        return tableauPharmaciens.stream()
            .mapToLong(TableauPharmacienDTO::getMontantAvoirFournisseur)
            .sum();
    }

    private FournisseurAchat createNewFournisseurAchat(Integer groupId, String libelle) {
        FournisseurAchat fournisseurAchat = new FournisseurAchat();
        fournisseurAchat.setId(groupId);
        fournisseurAchat.setLibelle(libelle);
        fournisseurAchat.setAchat(new AchatDTO());
        return fournisseurAchat;
    }
}
