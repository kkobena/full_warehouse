package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import com.kobe.warehouse.domain.enumeration.Periodicite;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "planification_facturation")
public class PlanificationFacturation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "periodicite", nullable = false, length = 20)
    private Periodicite periodicite;


    @Column(name = "derniere_periode_fin")
    private LocalDate dernierePeriodeFin;

    @NotNull
    @Column(name = "heure_declenchement", nullable = false)
    private LocalTime heureDeclenchement = LocalTime.of(10, 0);

    @Column(name = "facture_provisoire")
    private boolean factureProvisoire = false;

    @Column(name = "actif")
    private boolean actif = true;

    @Column(name = "prochaine_execution")
    private LocalDateTime prochaineExecution;

    @Column(name = "derniere_execution")
    private LocalDateTime derniereExecution;

    @Enumerated(EnumType.STRING)
    @Column(name = "dernier_statut", length = 20)
    private ExecutionStatut dernierStatut;

    @Column(name = "dernier_message", length = 500)
    private String dernierMessage;

    @NotNull
    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @NotNull
    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.created = now;
        this.updated = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updated = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public PlanificationFacturation setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public PlanificationFacturation setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public Periodicite getPeriodicite() {
        return periodicite;
    }

    public PlanificationFacturation setPeriodicite(Periodicite periodicite) {
        this.periodicite = periodicite;
        return this;
    }


    public LocalTime getHeureDeclenchement() {
        return heureDeclenchement;
    }

    public PlanificationFacturation setHeureDeclenchement(LocalTime heureDeclenchement) {
        this.heureDeclenchement = heureDeclenchement;
        return this;
    }





    public boolean isFactureProvisoire() {
        return factureProvisoire;
    }

    public PlanificationFacturation setFactureProvisoire(boolean factureProvisoire) {
        this.factureProvisoire = factureProvisoire;
        return this;
    }

    public boolean isActif() {
        return actif;
    }

    public PlanificationFacturation setActif(boolean actif) {
        this.actif = actif;
        return this;
    }

    public LocalDateTime getProchaineExecution() {
        return prochaineExecution;
    }

    public PlanificationFacturation setProchaineExecution(LocalDateTime prochaineExecution) {
        this.prochaineExecution = prochaineExecution;
        return this;
    }

    public LocalDateTime getDerniereExecution() {
        return derniereExecution;
    }

    public PlanificationFacturation setDerniereExecution(LocalDateTime derniereExecution) {
        this.derniereExecution = derniereExecution;
        return this;
    }

    public ExecutionStatut getDernierStatut() {
        return dernierStatut;
    }

    public PlanificationFacturation setDernierStatut(ExecutionStatut dernierStatut) {
        this.dernierStatut = dernierStatut;
        return this;
    }

    public String getDernierMessage() {
        return dernierMessage;
    }

    public PlanificationFacturation setDernierMessage(String dernierMessage) {
        this.dernierMessage = dernierMessage;
        return this;
    }

    public LocalDate getDernierePeriodeFin() {
        return dernierePeriodeFin;
    }

    public PlanificationFacturation setDernierePeriodeFin(LocalDate dernierePeriodeFin) {
        this.dernierePeriodeFin = dernierePeriodeFin;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }
}
