package com.kobe.warehouse.domain.ordonnance;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.enumeration.SourceOrdonnance;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

/**
 * Ordonnance : objet d'en-tête qui survit à la vente (reste à délivrer, renouvellement). Son statut
 * n'est pas stocké : il se dérive du reste à délivrer et de la date de fin de validité.
 */
@Entity
@Table(name = "ordonnance")
public class Ordonnance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescripteur_id")
    private Prescripteur prescripteur;

    @Column(name = "date_prescription", nullable = false)
    private LocalDate datePrescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private SourceOrdonnance source = SourceOrdonnance.MANUELLE;

    /** Renouvellements autorisés : la capacité d'une ligne est sa quantité prescrite x (renouvellements + 1). */
    @Column(name = "renouvellements", nullable = false)
    private short renouvellements;

    @Column(name = "date_fin_validite")
    private LocalDate dateFinValidite;

    /** Clôture décidée par une personne ; le reste du statut se dérive. */
    @Column(name = "cloturee", nullable = false)
    private boolean cloturee;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    @OneToMany(mappedBy = "ordonnance", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rang ASC")
    @BatchSize(size = 50)
    private List<OrdonnanceLigne> lignes = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Ordonnance setCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public Prescripteur getPrescripteur() {
        return prescripteur;
    }

    public Ordonnance setPrescripteur(Prescripteur prescripteur) {
        this.prescripteur = prescripteur;
        return this;
    }

    public LocalDate getDatePrescription() {
        return datePrescription;
    }

    public Ordonnance setDatePrescription(LocalDate datePrescription) {
        this.datePrescription = datePrescription;
        return this;
    }

    public SourceOrdonnance getSource() {
        return source;
    }

    public Ordonnance setSource(SourceOrdonnance source) {
        this.source = source;
        return this;
    }

    public int getRenouvellements() {
        return renouvellements;
    }

    public Ordonnance setRenouvellements(int renouvellements) {
        this.renouvellements = (short) renouvellements;
        return this;
    }

    public LocalDate getDateFinValidite() {
        return dateFinValidite;
    }

    public Ordonnance setDateFinValidite(LocalDate dateFinValidite) {
        this.dateFinValidite = dateFinValidite;
        return this;
    }

    public boolean isCloturee() {
        return cloturee;
    }

    public Ordonnance setCloturee(boolean cloturee) {
        this.cloturee = cloturee;
        return this;
    }

    public String getNote() {
        return note;
    }

    public Ordonnance setNote(String note) {
        this.note = note;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public Ordonnance setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public List<OrdonnanceLigne> getLignes() {
        return lignes;
    }

    public Ordonnance ajouterLigne(OrdonnanceLigne ligne) {
        ligne.setOrdonnance(this);
        lignes.add(ligne);
        return this;
    }
}
