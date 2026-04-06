package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
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
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "planification_certification_fne")
public class PlanificationCertificationFne implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @NotNull
    @Column(name = "heure_declenchement", nullable = false)
    private LocalTime heureDeclenchement = LocalTime.of(2, 0);

    @Column(name = "actif")
    private boolean actif = false;

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

    public Integer getId() { return id; }
    public PlanificationCertificationFne setId(Integer id) { this.id = id; return this; }

    public String getLibelle() { return libelle; }
    public PlanificationCertificationFne setLibelle(String libelle) { this.libelle = libelle; return this; }

    public LocalTime getHeureDeclenchement() { return heureDeclenchement; }
    public PlanificationCertificationFne setHeureDeclenchement(LocalTime heureDeclenchement) {
        this.heureDeclenchement = heureDeclenchement; return this;
    }

    public boolean isActif() { return actif; }
    public PlanificationCertificationFne setActif(boolean actif) { this.actif = actif; return this; }

    public LocalDateTime getProchaineExecution() { return prochaineExecution; }
    public PlanificationCertificationFne setProchaineExecution(LocalDateTime prochaineExecution) {
        this.prochaineExecution = prochaineExecution; return this;
    }

    public LocalDateTime getDerniereExecution() { return derniereExecution; }
    public PlanificationCertificationFne setDerniereExecution(LocalDateTime derniereExecution) {
        this.derniereExecution = derniereExecution; return this;
    }

    public ExecutionStatut getDernierStatut() { return dernierStatut; }
    public PlanificationCertificationFne setDernierStatut(ExecutionStatut dernierStatut) {
        this.dernierStatut = dernierStatut; return this;
    }

    public String getDernierMessage() { return dernierMessage; }
    public PlanificationCertificationFne setDernierMessage(String dernierMessage) {
        this.dernierMessage = dernierMessage; return this;
    }

    public LocalDateTime getCreated() { return created; }
    public LocalDateTime getUpdated() { return updated; }
}
