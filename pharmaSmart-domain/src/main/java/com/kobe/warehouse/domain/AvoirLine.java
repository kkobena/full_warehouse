package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "avoir_line")
public class AvoirLine implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "avoir_id", referencedColumnName = "id")
    private AvoirTiersPayant avoir;

    @NotNull
    @Column(name = "sale_line_id", nullable = false)
    private Long saleLineId;

    @NotNull
    @Column(name = "sale_line_date", nullable = false)
    private LocalDate saleLineDate;

    @NotNull
    @Column(name = "montant_avoir", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantAvoir = BigDecimal.ZERO;

    @Column(name = "motif_rejet", length = 200)
    private String motifRejet;

    public Long getId() {
        return id;
    }

    public AvoirLine setId(Long id) {
        this.id = id;
        return this;
    }

    public AvoirTiersPayant getAvoir() {
        return avoir;
    }

    public AvoirLine setAvoir(AvoirTiersPayant avoir) {
        this.avoir = avoir;
        return this;
    }

    public Long getSaleLineId() {
        return saleLineId;
    }

    public AvoirLine setSaleLineId(Long saleLineId) {
        this.saleLineId = saleLineId;
        return this;
    }

    public LocalDate getSaleLineDate() {
        return saleLineDate;
    }

    public AvoirLine setSaleLineDate(LocalDate saleLineDate) {
        this.saleLineDate = saleLineDate;
        return this;
    }

    public BigDecimal getMontantAvoir() {
        return montantAvoir;
    }

    public AvoirLine setMontantAvoir(BigDecimal montantAvoir) {
        this.montantAvoir = montantAvoir;
        return this;
    }

    public String getMotifRejet() {
        return motifRejet;
    }

    public AvoirLine setMotifRejet(String motifRejet) {
        this.motifRejet = motifRejet;
        return this;
    }
}
