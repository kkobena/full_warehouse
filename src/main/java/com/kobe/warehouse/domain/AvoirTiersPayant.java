package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "avoir_tiers_payant",
    indexes = {
        @Index(columnList = "avoir_date", name = "avoir_date_idx"),
        @Index(columnList = "statut", name = "avoir_statut_idx"),
    }
)
public class AvoirTiersPayant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "num_avoir", nullable = false, length = 30, unique = true)
    private String numAvoir;

    @NotNull
    @ManyToOne
    @JoinColumns(
        {
            @JoinColumn(name = "facture_origine_id", referencedColumnName = "id"),
            @JoinColumn(name = "facture_origine_date", referencedColumnName = "invoice_date"),
        }
    )
    private FactureTiersPayant factureTiersPayant;


    @NotNull
    @Column(name = "montant_avoir", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantAvoir = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 15, scale = 2)
    private BigDecimal montantTva = BigDecimal.ZERO;

    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHt = BigDecimal.ZERO;

    @Column(name = "motif", length = 500)
    private String motif;

    @NotNull
    @Column(name = "avoir_date", nullable = false)
    private LocalDate avoirDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 10)
    private AvoirStatut statut = AvoirStatut.DRAFT;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private AppUser user;

    @NotNull
    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @NotNull
    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;



    @OneToMany(mappedBy = "avoir", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvoirLine> lignes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.created = now;
        this.updated = now;
        if (this.avoirDate == null) {
            this.avoirDate = LocalDate.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updated = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AvoirTiersPayant setId(Long id) {
        this.id = id;
        return this;
    }

    public String getNumAvoir() {
        return numAvoir;
    }

    public AvoirTiersPayant setNumAvoir(String numAvoir) {
        this.numAvoir = numAvoir;
        return this;
    }

    public FactureTiersPayant getFactureTiersPayant() {
        return factureTiersPayant;
    }

    public AvoirTiersPayant setFactureTiersPayant(FactureTiersPayant factureTiersPayant) {
        this.factureTiersPayant = factureTiersPayant;
        return this;
    }

    public BigDecimal getMontantAvoir() {
        return montantAvoir;
    }

    public AvoirTiersPayant setMontantAvoir(BigDecimal montantAvoir) {
        this.montantAvoir = montantAvoir;
        return this;
    }

    public BigDecimal getMontantTva() {
        return montantTva;
    }

    public AvoirTiersPayant setMontantTva(BigDecimal montantTva) {
        this.montantTva = montantTva;
        return this;
    }

    public BigDecimal getMontantHt() {
        return montantHt;
    }

    public AvoirTiersPayant setMontantHt(BigDecimal montantHt) {
        this.montantHt = montantHt;
        return this;
    }

    public String getMotif() {
        return motif;
    }

    public AvoirTiersPayant setMotif(String motif) {
        this.motif = motif;
        return this;
    }

    public LocalDate getAvoirDate() {
        return avoirDate;
    }

    public AvoirTiersPayant setAvoirDate(LocalDate avoirDate) {
        this.avoirDate = avoirDate;
        return this;
    }

    public AvoirStatut getStatut() {
        return statut;
    }

    public AvoirTiersPayant setStatut(AvoirStatut statut) {
        this.statut = statut;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public AvoirTiersPayant setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public List<AvoirLine> getLignes() {
        return lignes;
    }

    public AvoirTiersPayant setLignes(List<AvoirLine> lignes) {
        this.lignes = lignes;
        return this;
    }
}
