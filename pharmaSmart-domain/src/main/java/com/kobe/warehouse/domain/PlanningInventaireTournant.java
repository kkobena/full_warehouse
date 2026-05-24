package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CritereTournant;
import com.kobe.warehouse.domain.enumeration.FrequenceTournant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Planning d'inventaire tournant (Cycle Counting). Un planning définit la fréquence et le critère
 * de rotation. Le job planifié ({@code TournantSchedulerService}) crée automatiquement un
 * {@link StoreInventory} à chaque échéance.
 */
@Entity
@Table(name = "planning_inventaire_tournant")
public class PlanningInventaireTournant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "frequence", nullable = false, length = 20)
    private FrequenceTournant frequence;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "critere", nullable = false, length = 30)
    private CritereTournant critere;

    /**
     * Storage de référence (scope du planning)
     */
    @ManyToOne
    private Storage storage;

    /**
     * Employé affecté à cet inventaire tournant. L'inventaire créé automatiquement lui sera
     * assigné. Si null, le premier utilisateur actif du système est utilisé (fallback).
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @NotNull
    @Column(name = "prochaine_execution", nullable = false)
    private LocalDate prochaineExecution;

    @NotNull
    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    /**
     * Index de rotation courant dans la liste ordonnée des critères.
     * <ul>
     *   <li>RAYON : index du rayon courant dans la liste triée des rayons du storage</li>
     *   <li>FAMILLE : index de la famille courante dans la liste triée des familles</li>
     *   <li>CLASSIFICATION_ABC : 0=A, 1=B, 2=C (cycle mod 3)</li>
     * </ul>
     */
    @Column(name = "critere_index_courant", nullable = false)
    private Integer critereIndexCourant = 0;

    /**
     * Pour critère CLASSIFICATION_ABC : la classe Pareto courante ('A', 'B' ou 'C')
     */
    @Column(name = "classe_pareto_courante", length = 1)
    private String classeParetoCourante;

    /**
     * Nombre total d'exécutions effectuées
     */
    @Column(name = "nb_executions", nullable = false)
    private Integer nbExecutions = 0;

    /**
     * Date de la dernière exécution
     */
    @Column(name = "derniere_execution")
    private LocalDate derniereExecution;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public PlanningInventaireTournant setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public FrequenceTournant getFrequence() {
        return frequence;
    }

    public PlanningInventaireTournant setFrequence(FrequenceTournant frequence) {
        this.frequence = frequence;
        return this;
    }

    public CritereTournant getCritere() {
        return critere;
    }

    public PlanningInventaireTournant setCritere(CritereTournant critere) {
        this.critere = critere;
        return this;
    }

    public Storage getStorage() {
        return storage;
    }

    public PlanningInventaireTournant setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public PlanningInventaireTournant setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public LocalDate getProchaineExecution() {
        return prochaineExecution;
    }

    public PlanningInventaireTournant setProchaineExecution(LocalDate prochaineExecution) {
        this.prochaineExecution = prochaineExecution;
        return this;
    }

    public Boolean getActif() {
        return actif;
    }

    public PlanningInventaireTournant setActif(Boolean actif) {
        this.actif = actif;
        return this;
    }

    public Integer getCritereIndexCourant() {
        return critereIndexCourant;
    }

    public PlanningInventaireTournant setCritereIndexCourant(Integer critereIndexCourant) {
        this.critereIndexCourant = critereIndexCourant;
        return this;
    }

    public String getClasseParetoCourante() {
        return classeParetoCourante;
    }

    public PlanningInventaireTournant setClasseParetoCourante(String classeParetoCourante) {
        this.classeParetoCourante = classeParetoCourante;
        return this;
    }

    public Integer getNbExecutions() {
        return nbExecutions;
    }

    public PlanningInventaireTournant setNbExecutions(Integer nbExecutions) {
        this.nbExecutions = nbExecutions;
        return this;
    }

    public LocalDate getDerniereExecution() {
        return derniereExecution;
    }

    public PlanningInventaireTournant setDerniereExecution(LocalDate derniereExecution) {
        this.derniereExecution = derniereExecution;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PlanningInventaireTournant setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public PlanningInventaireTournant setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlanningInventaireTournant)) {
            return false;
        }
        return id != null && id.equals(((PlanningInventaireTournant) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
