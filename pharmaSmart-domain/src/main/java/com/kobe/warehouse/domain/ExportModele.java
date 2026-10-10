package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.PeriodeRelative;
import com.kobe.warehouse.domain.enumeration.ScheduledReportFrequency;
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
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Modèle d'export enregistré : rejouable en un clic, et programmable (fréquence, heure, jour de la semaine ou du mois). Sa période
 * est relative (mois précédent…) et se recalcule à chaque exécution (migration V2.1.37).
 */
@Entity
@Table(name = "export_modele")
public class ExportModele implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "export", nullable = false, length = 40)
    private ExportDonnees export;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    private FormatExport format;

    /** Vide pour un export sans période (référentiel produits, clients). */
    @Enumerated(EnumType.STRING)
    @Column(name = "periode", length = 30)
    private PeriodeRelative periode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietaire_id")
    private AppUser proprietaire;

    @Column(name = "partage", nullable = false)
    private boolean partage;

    /** Vide : modèle non programmé. */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequence", length = 15)
    private ScheduledReportFrequency frequence;

    @Column(name = "heure")
    private LocalTime heure;

    /** Jour de la semaine (1 = lundi) pour une programmation hebdomadaire, du mois (1 à 28) pour une mensuelle. */
    @Column(name = "jour")
    private Integer jour;

    @Column(name = "prochaine_execution")
    private LocalDateTime prochaineExecution;

    public Long getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    public ExportModele setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public ExportDonnees getExport() {
        return export;
    }

    public ExportModele setExport(ExportDonnees export) {
        this.export = export;
        return this;
    }

    public FormatExport getFormat() {
        return format;
    }

    public ExportModele setFormat(FormatExport format) {
        this.format = format;
        return this;
    }

    public PeriodeRelative getPeriode() {
        return periode;
    }

    public ExportModele setPeriode(PeriodeRelative periode) {
        this.periode = periode;
        return this;
    }

    public AppUser getProprietaire() {
        return proprietaire;
    }

    public ExportModele setProprietaire(AppUser proprietaire) {
        this.proprietaire = proprietaire;
        return this;
    }

    public boolean isPartage() {
        return partage;
    }

    public ExportModele setPartage(boolean partage) {
        this.partage = partage;
        return this;
    }

    public ScheduledReportFrequency getFrequence() {
        return frequence;
    }

    public ExportModele setFrequence(ScheduledReportFrequency frequence) {
        this.frequence = frequence;
        return this;
    }

    public LocalTime getHeure() {
        return heure;
    }

    public ExportModele setHeure(LocalTime heure) {
        this.heure = heure;
        return this;
    }

    public Integer getJour() {
        return jour;
    }

    public ExportModele setJour(Integer jour) {
        this.jour = jour;
        return this;
    }

    public LocalDateTime getProchaineExecution() {
        return prochaineExecution;
    }

    public ExportModele setProchaineExecution(LocalDateTime prochaineExecution) {
        this.prochaineExecution = prochaineExecution;
        return this;
    }

    public boolean estProgramme() {
        return frequence != null;
    }
}
