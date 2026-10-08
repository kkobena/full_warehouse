package com.kobe.warehouse.domain.ordonnance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lien ligne d'ordonnance - ligne de vente. Il ne COMPTE comme délivrance que si la vente est
 * clôturée et non annulée : la quantité se lit sur la ligne de vente, à chaque lecture.
 */
@Entity
@Table(name = "ordonnance_delivrance")
public class OrdonnanceDelivrance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ligne_id", nullable = false)
    private Integer ligneId;

    @Column(name = "sales_line_id", nullable = false)
    private Long salesLineId;

    @Column(name = "sales_line_date", nullable = false)
    private LocalDate salesLineDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected OrdonnanceDelivrance() {}

    public OrdonnanceDelivrance(Integer ligneId, Long salesLineId, LocalDate salesLineDate) {
        this.ligneId = ligneId;
        this.salesLineId = salesLineId;
        this.salesLineDate = salesLineDate;
    }

    public Integer getId() {
        return id;
    }

    public Integer getLigneId() {
        return ligneId;
    }

    public Long getSalesLineId() {
        return salesLineId;
    }

    public LocalDate getSalesLineDate() {
        return salesLineDate;
    }
}
