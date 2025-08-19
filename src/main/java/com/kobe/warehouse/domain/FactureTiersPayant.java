package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "facture_tiers_payant",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "num_facture" }) },
    indexes = { @Index(columnList = "num_facture", name = "num_facture_index") }
)
public class FactureTiersPayant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "num_facture", nullable = false, length = 20)
    private String numFacture;

    private int remiseForfetaire;

    @ManyToOne
    @JoinColumn(name = "tiers_payant_id", referencedColumnName = "id")
    private TiersPayant tiersPayant;

    private boolean factureProvisoire;
    private LocalDate debutPeriode;
    private LocalDate finPeriode;

    @NotNull
    private LocalDateTime created;

    private LocalDateTime updated = LocalDateTime.now();

    @ColumnDefault("0")
    @Column(name = "montant_regle", columnDefinition = "int default '0'")
    private int montantRegle;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "varchar(20) default 'NOT_PAID'")
    private InvoiceStatut statut = InvoiceStatut.NOT_PAID;

    @ManyToOne
    @JoinColumn(name = "groupe_tiers_payant_id", referencedColumnName = "id")
    private GroupeTiersPayant groupeTiersPayant;

    @NotNull
    @ManyToOne(optional = false)
    private User user;

    @OneToMany(mappedBy = "groupeFactureTiersPayant")
    private List<FactureTiersPayant> factureTiersPayants = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "groupe_facture_tiers_payant_id", referencedColumnName = "id")
    private FactureTiersPayant groupeFactureTiersPayant;

    @OneToMany(mappedBy = "factureTiersPayant")
    private List<ThirdPartySaleLine> facturesDetails = new ArrayList<>();

    @Transient
    private String displayNumFacture;

    public LocalDateTime getCreated() {
        return created;
    }

    public FactureTiersPayant setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public String getDisplayNumFacture() {
        if (numFacture != null) {
            var parts = numFacture.split("_");
            if (parts.length == 2) {
                displayNumFacture = parts[1];
            } else {
                displayNumFacture = numFacture;
            }
        }
        return displayNumFacture;
    }

    public FactureTiersPayant setDisplayNumFacture(String displayNumFacture) {
        this.displayNumFacture = displayNumFacture;
        return this;
    }

    public LocalDate getDebutPeriode() {
        return debutPeriode;
    }

    public FactureTiersPayant setDebutPeriode(LocalDate debutPeriode) {
        this.debutPeriode = debutPeriode;
        return this;
    }

    public boolean isFactureProvisoire() {
        return factureProvisoire;
    }

    public FactureTiersPayant setFactureProvisoire(boolean factureProvisoire) {
        this.factureProvisoire = factureProvisoire;
        return this;
    }

    public List<ThirdPartySaleLine> getFacturesDetails() {
        return facturesDetails;
    }

    public FactureTiersPayant setFacturesDetails(List<ThirdPartySaleLine> facturesDetails) {
        this.facturesDetails = facturesDetails;
        return this;
    }

    public List<FactureTiersPayant> getFactureTiersPayants() {
        return factureTiersPayants;
    }

    public FactureTiersPayant setFactureTiersPayants(List<FactureTiersPayant> factureTiersPayants) {
        this.factureTiersPayants = factureTiersPayants;
        return this;
    }

    public LocalDate getFinPeriode() {
        return finPeriode;
    }

    public FactureTiersPayant setFinPeriode(LocalDate finPeriode) {
        this.finPeriode = finPeriode;
        return this;
    }

    public FactureTiersPayant getGroupeFactureTiersPayant() {
        return groupeFactureTiersPayant;
    }

    public FactureTiersPayant setGroupeFactureTiersPayant(FactureTiersPayant groupeFactureTiersPayant) {
        this.groupeFactureTiersPayant = groupeFactureTiersPayant;
        return this;
    }

    public GroupeTiersPayant getGroupeTiersPayant() {
        return groupeTiersPayant;
    }

    public FactureTiersPayant setGroupeTiersPayant(GroupeTiersPayant groupeTiersPayant) {
        this.groupeTiersPayant = groupeTiersPayant;
        return this;
    }

    public Long getId() {
        return id;
    }

    public FactureTiersPayant setId(Long id) {
        this.id = id;
        return this;
    }

    public int getMontantRegle() {
        return montantRegle;
    }

    public FactureTiersPayant setMontantRegle(int montantRegle) {
        this.montantRegle = montantRegle;
        return this;
    }

    public int getRemiseForfetaire() {
        return remiseForfetaire;
    }

    public FactureTiersPayant setRemiseForfetaire(int remiseForfetaire) {
        this.remiseForfetaire = remiseForfetaire;
        return this;
    }

    public @NotNull User getUser() {
        return user;
    }

    public FactureTiersPayant setUser(@NotNull User user) {
        this.user = user;
        return this;
    }

    public String getNumFacture() {
        return numFacture;
    }

    public FactureTiersPayant setNumFacture(String numFacture) {
        this.numFacture = numFacture;
        return this;
    }

    public @NotNull InvoiceStatut getStatut() {
        return statut;
    }

    public FactureTiersPayant setStatut(@NotNull InvoiceStatut statut) {
        this.statut = statut;
        return this;
    }

    public TiersPayant getTiersPayant() {
        return tiersPayant;
    }

    public FactureTiersPayant setTiersPayant(TiersPayant tiersPayant) {
        this.tiersPayant = tiersPayant;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public FactureTiersPayant setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }
}
