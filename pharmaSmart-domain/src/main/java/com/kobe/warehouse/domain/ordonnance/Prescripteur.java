package com.kobe.warehouse.domain.ordonnance;

import com.kobe.warehouse.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Prescripteur du référentiel local, constitué par l'usage ; jamais supprimé, {@code actif} ou fusionné. */
@Entity
@Table(name = "prescripteur")
public class Prescripteur implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", length = 100)
    private String prenom;

    @Column(name = "specialite", length = 100)
    private String specialite;

    @Column(name = "numero_ordre", length = 30)
    private String numeroOrdre;

    @Column(name = "structure", length = 150)
    private String structure;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /** Colonne générée par la base (ref_normaliser du nom et du prénom) : sert à la recherche. */
    @Column(name = "recherche", insertable = false, updatable = false)
    private String recherche;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    public Integer getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Prescripteur setNom(String nom) {
        this.nom = nom;
        return this;
    }

    public String getPrenom() {
        return prenom;
    }

    public Prescripteur setPrenom(String prenom) {
        this.prenom = prenom;
        return this;
    }

    public String getSpecialite() {
        return specialite;
    }

    public Prescripteur setSpecialite(String specialite) {
        this.specialite = specialite;
        return this;
    }

    public String getNumeroOrdre() {
        return numeroOrdre;
    }

    public Prescripteur setNumeroOrdre(String numeroOrdre) {
        this.numeroOrdre = numeroOrdre;
        return this;
    }

    public String getStructure() {
        return structure;
    }

    public Prescripteur setStructure(String structure) {
        this.structure = structure;
        return this;
    }

    public String getTelephone() {
        return telephone;
    }

    public Prescripteur setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public boolean isActif() {
        return actif;
    }

    public Prescripteur setActif(boolean actif) {
        this.actif = actif;
        return this;
    }

    public String getRecherche() {
        return recherche;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public Prescripteur setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
        return this;
    }
}
