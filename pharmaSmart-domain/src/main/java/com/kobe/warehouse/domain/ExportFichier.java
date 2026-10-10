package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.StatutExport;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un export demandé : le fichier généré en tâche de fond et sa trace (qui, quand, quoi, sur quelle période, combien de lignes),
 * gardée après expiration du fichier — obligatoire dès qu'un export contient des données personnelles (migration V2.1.37).
 */
@Entity
@Table(name = "export_fichier")
public class ExportFichier implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "export", nullable = false, length = 40)
    private ExportDonnees export;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    private FormatExport format;

    @Column(name = "du")
    private LocalDate du;

    @Column(name = "au")
    private LocalDate au;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutExport statut;

    @Column(name = "nombre_lignes")
    private Long nombreLignes;

    @Column(name = "taille")
    private Long taille;

    /** Nom du fichier dans le répertoire des exports ; jamais un chemin fourni par l'utilisateur. */
    @Column(name = "fichier", length = 150)
    private String fichier;

    @Column(name = "erreur", length = 500)
    private String erreur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_par_id")
    private AppUser demandePar;

    @Column(name = "demande_le", nullable = false)
    private LocalDateTime demandeLe;

    @Column(name = "termine_le")
    private LocalDateTime termineLe;

    @Column(name = "expire_le")
    private LocalDateTime expireLe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modele_id")
    private ExportModele modele;

    public Long getId() {
        return id;
    }

    public ExportDonnees getExport() {
        return export;
    }

    public ExportFichier setExport(ExportDonnees export) {
        this.export = export;
        return this;
    }

    public FormatExport getFormat() {
        return format;
    }

    public ExportFichier setFormat(FormatExport format) {
        this.format = format;
        return this;
    }

    public LocalDate getDu() {
        return du;
    }

    public ExportFichier setDu(LocalDate du) {
        this.du = du;
        return this;
    }

    public LocalDate getAu() {
        return au;
    }

    public ExportFichier setAu(LocalDate au) {
        this.au = au;
        return this;
    }

    public StatutExport getStatut() {
        return statut;
    }

    public ExportFichier setStatut(StatutExport statut) {
        this.statut = statut;
        return this;
    }

    public Long getNombreLignes() {
        return nombreLignes;
    }

    public ExportFichier setNombreLignes(Long nombreLignes) {
        this.nombreLignes = nombreLignes;
        return this;
    }

    public Long getTaille() {
        return taille;
    }

    public ExportFichier setTaille(Long taille) {
        this.taille = taille;
        return this;
    }

    public String getFichier() {
        return fichier;
    }

    public ExportFichier setFichier(String fichier) {
        this.fichier = fichier;
        return this;
    }

    public String getErreur() {
        return erreur;
    }

    public ExportFichier setErreur(String erreur) {
        this.erreur = erreur;
        return this;
    }

    public AppUser getDemandePar() {
        return demandePar;
    }

    public ExportFichier setDemandePar(AppUser demandePar) {
        this.demandePar = demandePar;
        return this;
    }

    public LocalDateTime getDemandeLe() {
        return demandeLe;
    }

    public ExportFichier setDemandeLe(LocalDateTime demandeLe) {
        this.demandeLe = demandeLe;
        return this;
    }

    public LocalDateTime getTermineLe() {
        return termineLe;
    }

    public ExportFichier setTermineLe(LocalDateTime termineLe) {
        this.termineLe = termineLe;
        return this;
    }

    public LocalDateTime getExpireLe() {
        return expireLe;
    }

    public ExportFichier setExpireLe(LocalDateTime expireLe) {
        this.expireLe = expireLe;
        return this;
    }

    public ExportModele getModele() {
        return modele;
    }

    public ExportFichier setModele(ExportModele modele) {
        this.modele = modele;
        return this;
    }
}
