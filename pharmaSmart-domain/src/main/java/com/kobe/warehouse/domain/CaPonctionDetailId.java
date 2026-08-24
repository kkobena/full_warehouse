package com.kobe.warehouse.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Clé de {@link CaPonctionDetail} : la ponction et la vente touchée.
 *
 * <p>La date fait partie de la clé parce que {@code sales} est partitionnée sur {@code sale_date} —
 * l'identifiant d'une vente ne suffit pas à la désigner.
 */
public class CaPonctionDetailId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer ponction;
    private Long saleId;
    private LocalDate saleDate;

    public CaPonctionDetailId() {}

    public CaPonctionDetailId(Integer ponction, Long saleId, LocalDate saleDate) {
        this.ponction = ponction;
        this.saleId = saleId;
        this.saleDate = saleDate;
    }

    public Integer getPonction() {
        return ponction;
    }

    public void setPonction(Integer ponction) {
        this.ponction = ponction;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CaPonctionDetailId autre = (CaPonctionDetailId) o;
        return (
            Objects.equals(ponction, autre.ponction) &&
            Objects.equals(saleId, autre.saleId) &&
            Objects.equals(saleDate, autre.saleDate)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(ponction, saleId, saleDate);
    }
}
