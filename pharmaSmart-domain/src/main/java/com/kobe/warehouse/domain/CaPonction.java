package com.kobe.warehouse.domain;

import com.kobe.warehouse.service.declaration_ca.dto.ModeCalculPonction;
import com.kobe.warehouse.service.declaration_ca.dto.StatutPonction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une ponction du chiffre d'affaires à déclarer, sur une période.
 *
 * <p>L'entité sert les <strong>lectures</strong> — historique, justificatif, contrôle de statut. Les
 * écritures qui touchent des milliers de ventes restent ensemblistes, en SQL : les charger pour les
 * modifier une à une serait intenable sur une période de deux mois.
 *
 * <p>Les montants sont figés à la validation et ne sont plus recalculés : ils disent ce qui a été
 * décidé ce jour-là, pas ce que la même règle donnerait aujourd'hui. C'est ce qui rend l'historique
 * opposable.
 */
@Entity
@Table(name = "ca_ponction")
public class CaPonction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Attribué par {@code id_ca_ponction_seq}, comme les autres entités du domaine — d'où l'absence
     * de {@code @GeneratedValue} : c'est le service qui appelle la séquence.
     */
    @Id
    private Integer id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Magasin magasin;

    @NotNull
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @NotNull
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "mode_calcul", nullable = false, length = 15)
    private ModeCalculPonction modeCalcul;

    /** Montant en francs ou taux en pourcentage, selon {@link #modeCalcul}. */
    @NotNull
    @Column(name = "valeur_saisie", nullable = false, precision = 12, scale = 2)
    private BigDecimal valeurSaisie;

    @NotNull
    @Column(name = "plafond_par_vente", nullable = false, precision = 5, scale = 2)
    private BigDecimal plafondParVente;

    @Column(name = "strategie", nullable = false, length = 15)
    private String strategie;

    @Column(name = "modes_reglement", nullable = false, length = 80)
    private String modesReglement;

    @Column(name = "taux_tva_eligibles", nullable = false, length = 40)
    private String tauxTvaEligibles;

    @Column(name = "ca_reel", nullable = false)
    private long caReel;

    @Column(name = "ca_apres_exclusions", nullable = false)
    private long caApresExclusions;

    @Column(name = "ca_assiette_tva0", nullable = false)
    private long caAssietteTva0;

    @Column(name = "montant_ponctionnable", nullable = false)
    private long montantPonctionnable;

    @Column(name = "montant_objectif", nullable = false)
    private long montantObjectif;

    @Column(name = "montant_ponctionne", nullable = false)
    private long montantPonctionne;

    @Column(name = "ca_declare", nullable = false)
    private long caDeclare;

    @Column(name = "nombre_ventes", nullable = false)
    private int nombreVentes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 15)
    private StatutPonction statut;

    /** Modules souscrits au moment du calcul, pour qu'un audit sache ce qui était autorisé. */
    @Column(name = "features_actives", length = 120)
    private String featuresActives;

    @Column(name = "commentaire")
    private String commentaire;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private AppUser validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canceled_by")
    private AppUser canceledBy;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    public Integer getId() {
        return id;
    }

    public CaPonction setId(Integer id) {
        this.id = id;
        return this;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public CaPonction setMagasin(Magasin magasin) {
        this.magasin = magasin;
        return this;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public CaPonction setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
        return this;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public CaPonction setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
        return this;
    }

    public ModeCalculPonction getModeCalcul() {
        return modeCalcul;
    }

    public CaPonction setModeCalcul(ModeCalculPonction modeCalcul) {
        this.modeCalcul = modeCalcul;
        return this;
    }

    public BigDecimal getValeurSaisie() {
        return valeurSaisie;
    }

    public CaPonction setValeurSaisie(BigDecimal valeurSaisie) {
        this.valeurSaisie = valeurSaisie;
        return this;
    }

    public BigDecimal getPlafondParVente() {
        return plafondParVente;
    }

    public CaPonction setPlafondParVente(BigDecimal plafondParVente) {
        this.plafondParVente = plafondParVente;
        return this;
    }

    public String getStrategie() {
        return strategie;
    }

    public CaPonction setStrategie(String strategie) {
        this.strategie = strategie;
        return this;
    }

    public String getModesReglement() {
        return modesReglement;
    }

    public CaPonction setModesReglement(String modesReglement) {
        this.modesReglement = modesReglement;
        return this;
    }

    public String getTauxTvaEligibles() {
        return tauxTvaEligibles;
    }

    public CaPonction setTauxTvaEligibles(String tauxTvaEligibles) {
        this.tauxTvaEligibles = tauxTvaEligibles;
        return this;
    }

    public long getCaReel() {
        return caReel;
    }

    public CaPonction setCaReel(long caReel) {
        this.caReel = caReel;
        return this;
    }

    public long getCaApresExclusions() {
        return caApresExclusions;
    }

    public CaPonction setCaApresExclusions(long caApresExclusions) {
        this.caApresExclusions = caApresExclusions;
        return this;
    }

    public long getCaAssietteTva0() {
        return caAssietteTva0;
    }

    public CaPonction setCaAssietteTva0(long caAssietteTva0) {
        this.caAssietteTva0 = caAssietteTva0;
        return this;
    }

    public long getMontantPonctionnable() {
        return montantPonctionnable;
    }

    public CaPonction setMontantPonctionnable(long montantPonctionnable) {
        this.montantPonctionnable = montantPonctionnable;
        return this;
    }

    public long getMontantObjectif() {
        return montantObjectif;
    }

    public CaPonction setMontantObjectif(long montantObjectif) {
        this.montantObjectif = montantObjectif;
        return this;
    }

    public long getMontantPonctionne() {
        return montantPonctionne;
    }

    public CaPonction setMontantPonctionne(long montantPonctionne) {
        this.montantPonctionne = montantPonctionne;
        return this;
    }

    public long getCaDeclare() {
        return caDeclare;
    }

    public CaPonction setCaDeclare(long caDeclare) {
        this.caDeclare = caDeclare;
        return this;
    }

    public int getNombreVentes() {
        return nombreVentes;
    }

    public CaPonction setNombreVentes(int nombreVentes) {
        this.nombreVentes = nombreVentes;
        return this;
    }

    public StatutPonction getStatut() {
        return statut;
    }

    public CaPonction setStatut(StatutPonction statut) {
        this.statut = statut;
        return this;
    }

    public String getFeaturesActives() {
        return featuresActives;
    }

    public CaPonction setFeaturesActives(String featuresActives) {
        this.featuresActives = featuresActives;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public CaPonction setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public CaPonction setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CaPonction setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public AppUser getValidatedBy() {
        return validatedBy;
    }

    public CaPonction setValidatedBy(AppUser validatedBy) {
        this.validatedBy = validatedBy;
        return this;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public CaPonction setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
        return this;
    }

    public AppUser getCanceledBy() {
        return canceledBy;
    }

    public CaPonction setCanceledBy(AppUser canceledBy) {
        this.canceledBy = canceledBy;
        return this;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public CaPonction setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
        return this;
    }
}
