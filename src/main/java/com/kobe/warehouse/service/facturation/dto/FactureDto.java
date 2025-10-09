package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.util.StringUtils;

public class FactureDto extends FactureDtoWrapper {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private String numFacture;
    private String tiersPayantName;
    private Long factureId;
    private Long groupeFactureId;
    private String groupeNumFacture;
    private Integer montantRegle;
    private Integer montant;
    private Integer remiseForfetaire;
    private Integer montantVente;
    private Integer montantRemiseVente;
    private Integer montantNetVente;
    private Integer montantNet;
    private LocalDateTime created;
    private Long itemsCount;
    private Integer montantAttendu;
    private Integer itemMontantRegle;
    private Integer montantRestant;
    private LocalDate debutPeriode;
    private LocalDate finPeriode;
    private Boolean factureProvisoire;
    private String periode;
    private InvoiceStatut statut;
    private List<FactureItemDto> items;

    public FactureDto() {}

    public FactureDto(
        LocalDate invoiceDate,
        LocalDateTime created,
        Long id,
        Long groupeFactureId,
        Integer montantRegle,
        Integer remiseForfetaire,
        LocalDate debutPeriode,
        InvoiceStatut statut,
        LocalDate finPeriode,
        Boolean factureProvisoire,
        String numFacture,
        String groupeNumFacture,
        String tiersPayantName,
        Integer montantVente,
        Integer montantRemiseVente,
        Integer itemMontantRegle,
        Long itemsCount,
        Integer montant
    ) {
        this.created = created;
        this.factureId = id;
        this.factureItemId = new FactureItemId(id, invoiceDate);
        this.groupeFactureId = groupeFactureId;
        this.montantRegle = montantRegle;
        this.remiseForfetaire = remiseForfetaire;
        this.debutPeriode = debutPeriode;
        this.statut = statut;
        this.finPeriode = finPeriode;
        this.factureProvisoire = factureProvisoire;
        this.numFacture = numFacture;
        this.groupeNumFacture = groupeNumFacture;
        this.tiersPayantName = tiersPayantName;
        this.montantVente = montantVente;
        this.montantRemiseVente = montantRemiseVente;
        this.itemMontantRegle = itemMontantRegle;
        this.itemsCount = itemsCount;
        this.montant = montant;
    }

    public FactureDto(
        LocalDate invoiceDate,
        Integer montantRegle,
        LocalDateTime created,
        Long id,
        LocalDate debutPeriode,
        InvoiceStatut statut,
        LocalDate finPeriode,
        Boolean factureProvisoire,
        String groupeNumFacture,
        String tiersPayantName,
        Integer montantVente,
        Long itemsCount,
        Integer montant,
        Integer montantRemiseVente
    ) {
        this.factureItemId = new FactureItemId(id, invoiceDate);
        this.montantRegle = montantRegle;
        this.created = created;
        this.factureId = id;
        this.debutPeriode = debutPeriode;
        this.statut = statut;
        this.finPeriode = finPeriode;
        this.factureProvisoire = factureProvisoire;
        this.tiersPayantName = tiersPayantName;
        this.groupeNumFacture = groupeNumFacture;
        this.montantVente = montantVente;
        this.itemsCount = itemsCount;
        this.montant = montant;
        this.montantRemiseVente = montantRemiseVente;
    }

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

    public String getNumFacture() {
        if (StringUtils.hasText(numFacture)) {
            String[] numFactureParts = numFacture.split("_");
            if (numFactureParts.length > 1) {
                numFacture = numFactureParts[1];
            } else {
                numFacture = numFactureParts[0];
            }
        }
        return numFacture;
    }

    public FactureDto setNumFacture(String numFacture) {
        this.numFacture = numFacture;
        return this;
    }

    public String getTiersPayantName() {
        return tiersPayantName;
    }

    public FactureDto setTiersPayantName(String tiersPayantName) {
        this.tiersPayantName = tiersPayantName;
        return this;
    }

    public Long getFactureId() {
        return factureId;
    }

    public FactureDto setFactureId(Long factureId) {
        this.factureId = factureId;
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

    public Integer getMontantRegle() {
        return montantRegle;
    }

    public FactureDto setMontantRegle(Integer montantRegle) {
        this.montantRegle = montantRegle;
        return this;
    }

    public Integer getMontant() {
        return montant;
    }

    public FactureDto setMontant(Integer montant) {
        this.montant = montant;
        return this;
    }

    public Integer getRemiseForfetaire() {
        return remiseForfetaire;
    }

    public FactureDto setRemiseForfetaire(Integer remiseForfetaire) {
        this.remiseForfetaire = remiseForfetaire;
        return this;
    }

    public Integer getMontantVente() {
        return montantVente;
    }

    public FactureDto setMontantVente(Integer montantVente) {
        this.montantVente = montantVente;
        return this;
    }

    public Integer getMontantRemiseVente() {
        return montantRemiseVente;
    }

    public FactureDto setMontantRemiseVente(Integer montantRemiseVente) {
        this.montantRemiseVente = montantRemiseVente;
        return this;
    }

    public Integer getMontantNetVente() {
        montantNetVente = Objects.requireNonNullElse(montantVente, 0) - Objects.requireNonNullElse(montantRemiseVente, 0);
        return montantNetVente;
    }

    public FactureDto setMontantNetVente(Integer montantNetVente) {
        this.montantNetVente = montantNetVente;
        return this;
    }

    public Integer getMontantNet() {
        montantNet = Objects.requireNonNullElse(montant, 0) - Objects.requireNonNullElse(remiseForfetaire, 0);
        return montantNet;
    }

    public FactureDto setMontantNet(Integer montantNet) {
        this.montantNet = montantNet;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public FactureDto setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public Long getItemsCount() {
        return itemsCount;
    }

    public FactureDto setItemsCount(Long itemsCount) {
        this.itemsCount = itemsCount;
        return this;
    }

    public Integer getMontantAttendu() {
        return montantAttendu;
    }

    public FactureDto setMontantAttendu(Integer montantAttendu) {
        this.montantAttendu = montantAttendu;
        return this;
    }

    public Integer getItemMontantRegle() {
        return itemMontantRegle;
    }

    public FactureDto setItemMontantRegle(Integer itemMontantRegle) {
        this.itemMontantRegle = itemMontantRegle;
        return this;
    }

    public Integer getMontantRestant() {
        montantRestant = Objects.requireNonNullElse(montantNet, 0) - Objects.requireNonNullElse(montantRegle, 0);
        return montantRestant;
    }

    public FactureDto setMontantRestant(Integer montantRestant) {
        this.montantRestant = montantRestant;
        return this;
    }

    public LocalDate getDebutPeriode() {
        return debutPeriode;
    }

    public FactureDto setDebutPeriode(LocalDate debutPeriode) {
        this.debutPeriode = debutPeriode;
        return this;
    }

    public LocalDate getFinPeriode() {
        return finPeriode;
    }

    public FactureDto setFinPeriode(LocalDate finPeriode) {
        this.finPeriode = finPeriode;
        return this;
    }

    public Boolean getFactureProvisoire() {
        return factureProvisoire;
    }

    public FactureDto setFactureProvisoire(Boolean factureProvisoire) {
        this.factureProvisoire = factureProvisoire;
        return this;
    }

    public String getPeriode() {
        if (debutPeriode != null && finPeriode != null) {
            periode = "Du " + debutPeriode.format(formatter) + " au " + finPeriode.format(formatter);
        }
        return periode;
    }

    public FactureDto setPeriode(String periode) {
        this.periode = periode;
        return this;
    }

    public InvoiceStatut getStatut() {
        return statut;
    }

    public FactureDto setStatut(InvoiceStatut statut) {
        this.statut = statut;
        return this;
    }
}
