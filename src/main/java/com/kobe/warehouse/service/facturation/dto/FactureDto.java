package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FactureDto extends FactureDtoWrapper {

    private String numFacture;
    private String tiersPayantName;
    private Long factureId;
    private Long groupeFactureId;
    private String groupeNumFacture;
    private Integer montantRegle;
    private Long montant;
    private Long remiseForfetaire;
    private Long montantVente;
    private Long montantRemiseVente;
    private Long montantNetVente;
    private Long montantNet;
    private LocalDateTime created;
    private long itemsCount;
    private long montantAttendu;
    private long itemMontantRegle;
    private long montantRestant;
    private LocalDate debutPeriode;
    private LocalDate finPeriode;
    private boolean factureProvisoire;
    private String periode;
    private InvoiceStatut statut;
    private List<FactureItemDto> items;

    public List<FactureItemDto> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public FactureDto setItems(List<FactureItemDto> items) {
        this.items = items;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public FactureDto setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public long getMontantRestant() {
        return montantRestant;
    }

    public FactureDto setMontantRestant(long montantRestant) {
        this.montantRestant = montantRestant;
        return this;
    }

    public InvoiceStatut getStatut() {
        return statut;
    }

    public FactureDto setStatut(InvoiceStatut statut) {
        this.statut = statut;
        return this;
    }

    public LocalDate getDebutPeriode() {
        return debutPeriode;
    }

    public FactureDto setDebutPeriode(LocalDate debutPeriode) {
        this.debutPeriode = debutPeriode;
        return this;
    }

    public Long getFactureId() {
        return factureId;
    }

    public FactureDto setFactureId(Long factureId) {
        this.factureId = factureId;
        return this;
    }

    public boolean isFactureProvisoire() {
        return factureProvisoire;
    }

    public FactureDto setFactureProvisoire(boolean factureProvisoire) {
        this.factureProvisoire = factureProvisoire;
        return this;
    }

    public LocalDate getFinPeriode() {
        return finPeriode;
    }

    public FactureDto setFinPeriode(LocalDate finPeriode) {
        this.finPeriode = finPeriode;
        return this;
    }

    public Long getGroupeFactureId() {
        return groupeFactureId;
    }

    public FactureDto setGroupeFactureId(Long groupeFactureId) {
        this.groupeFactureId = groupeFactureId;
        return this;
    }

    public String getGroupeNumFacture() {
        return groupeNumFacture;
    }

    public FactureDto setGroupeNumFacture(String groupeNumFacture) {
        this.groupeNumFacture = groupeNumFacture;
        return this;
    }

    public long getItemsCount() {
        return itemsCount;
    }

    public FactureDto setItemsCount(long itemsCount) {
        this.itemsCount = itemsCount;
        return this;
    }

    public Long getMontant() {
        if (montant == null) {
            montant = 0L;
        }
        return montant;
    }

    public FactureDto setMontant(Long montant) {
        this.montant = montant;
        return this;
    }

    public long getMontantAttendu() {
        return montantAttendu;
    }

    public FactureDto setMontantAttendu(long montantAttendu) {
        this.montantAttendu = montantAttendu;
        return this;
    }

    public Long getMontantNet() {
        return montantNet;
    }

    public FactureDto setMontantNet(Long montantNet) {
        this.montantNet = montantNet;
        return this;
    }

    public Long getMontantNetVente() {
        return montantNetVente;
    }

    public FactureDto setMontantNetVente(Long montantNetVente) {
        this.montantNetVente = montantNetVente;
        return this;
    }

    public Integer getMontantRegle() {
        return montantRegle;
    }

    public FactureDto setMontantRegle(Integer montantRegle) {
        this.montantRegle = montantRegle;
        return this;
    }

    public Long getMontantRemiseVente() {
        return montantRemiseVente;
    }

    public FactureDto setMontantRemiseVente(Long montantRemiseVente) {
        this.montantRemiseVente = montantRemiseVente;
        return this;
    }

    public Long getMontantVente() {
        return montantVente;
    }

    public FactureDto setMontantVente(Long montantVente) {
        this.montantVente = montantVente;
        return this;
    }

    public String getNumFacture() {
        return numFacture;
    }

    public FactureDto setNumFacture(String numFacture) {
        this.numFacture = numFacture;
        return this;
    }

    public String getPeriode() {
        return periode;
    }

    public FactureDto setPeriode(String periode) {
        this.periode = periode;
        return this;
    }

    public Long getRemiseForfetaire() {
        return remiseForfetaire;
    }

    public FactureDto setRemiseForfetaire(Long remiseForfetaire) {
        this.remiseForfetaire = remiseForfetaire;
        return this;
    }

    public String getTiersPayantName() {
        return tiersPayantName;
    }

    public FactureDto setTiersPayantName(String tiersPayantName) {
        this.tiersPayantName = tiersPayantName;
        return this;
    }

    public long getItemMontantRegle() {
        return itemMontantRegle;
    }

    public FactureDto setItemMontantRegle(long itemMontantRegle) {
        this.itemMontantRegle = itemMontantRegle;
        return this;
    }
}
