package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Une décision commerciale datée, sur un périmètre d'articles, qui fait payer moins que le prix
 * affiché sans que personne ait à la saisir au comptoir. Elle ne change jamais le prix : le moteur
 * en tire une remise de ligne .
 */
@Entity
@Table(name = "promotion")
public class Promotion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 20, unique = true)
    private String code;

    @Column(name = "libelle", length = 100, nullable = false)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private TypePromotion type;

    @Column(name = "valeur")
    private Integer valeur;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20, nullable = false)
    private StatutPromotion statut = StatutPromotion.BROUILLON;

    /**
     * Départage, pas décision : le moteur retient toujours la promotion la plus avantageuse pour la
     * cliente (une seule par ligne, pas de cumul entre promotions). La priorité, décroissante, puis
     * l'identifiant ne tranchent qu'entre deux promotions de ligne de même effet. Elle fixe aussi
     * l'ordre d'examen des promotions de groupe (offre N+M, pack, seuil en montant) : la première
     * qui prend une ligne la garde. Jamais l'ordre de création . 0 par défaut.
     */
    @Column(name = "priorite", nullable = false)
    private int priorite;

    @Column(name = "quantite_min", nullable = false)
    private int quantiteMin = 1;

    @Column(name = "quantite_max_par_vente")
    private Integer quantiteMaxParVente;

    /**
     * « Dans la limite de N unités » : toutes ventes clôturées et non annulées confondues.
     */
    @Column(name = "quantite_max_totale")
    private Integer quantiteMaxTotale;

    /**
     * Code-barres imprimé sur l'emballage d'un pack : au scan, le comptoir ajoute ses articles.
     */
    @Column(name = "code_barre_pack", length = 20)
    private String codeBarrePack;

    /**
     * Montant d'achat, en francs, des articles de la promotion qui déclenche une remise de panier.
     */
    @Column(name = "seuil_montant")
    private Integer seuilMontant;

    /**
     * S'applique aussi sur une ligne déjà remisée par la remise produit de la vente, après elle.
     * Mécaniques de ligne seulement ; faux par défaut.
     */
    @Column(name = "cumulable", nullable = false)
    private boolean cumulable;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "promotion_magasin",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "magasin_id"))
    private Set<Magasin> magasins = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "promotion_produit", joinColumns = @JoinColumn(name = "promotion_id"))
    private Set<PromotionProduit> produits = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Vrai si la promotion est validée et que {@code date} tombe entre ses bornes, incluses.
     */
    public boolean estEnVigueurLe(LocalDate date) {
        return statut == StatutPromotion.VALIDEE && !date.isBefore(dateDebut) && !date.isAfter(dateFin);
    }

    public Long getId() {
        return id;
    }

    public Promotion setId(Long id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Promotion setCode(String code) {
        this.code = code;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public Promotion setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public TypePromotion getType() {
        return type;
    }

    public Promotion setType(TypePromotion type) {
        this.type = type;
        return this;
    }

    public Integer getValeur() {
        return valeur;
    }

    public Promotion setValeur(Integer valeur) {
        this.valeur = valeur;
        return this;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public Promotion setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
        return this;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public Promotion setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
        return this;
    }

    public StatutPromotion getStatut() {
        return statut;
    }

    public Promotion setStatut(StatutPromotion statut) {
        this.statut = statut;
        return this;
    }

    public int getPriorite() {
        return priorite;
    }

    public Promotion setPriorite(int priorite) {
        this.priorite = priorite;
        return this;
    }

    public int getQuantiteMin() {
        return quantiteMin;
    }

    public Promotion setQuantiteMin(int quantiteMin) {
        this.quantiteMin = quantiteMin;
        return this;
    }

    public Integer getQuantiteMaxParVente() {
        return quantiteMaxParVente;
    }

    public Promotion setQuantiteMaxParVente(Integer quantiteMaxParVente) {
        this.quantiteMaxParVente = quantiteMaxParVente;
        return this;
    }

    public Integer getQuantiteMaxTotale() {
        return quantiteMaxTotale;
    }

    public Promotion setQuantiteMaxTotale(Integer quantiteMaxTotale) {
        this.quantiteMaxTotale = quantiteMaxTotale;
        return this;
    }

    public boolean isCumulable() {
        return cumulable;
    }

    public Promotion setCumulable(boolean cumulable) {
        this.cumulable = cumulable;
        return this;
    }

    public Integer getSeuilMontant() {
        return seuilMontant;
    }

    public Promotion setSeuilMontant(Integer seuilMontant) {
        this.seuilMontant = seuilMontant;
        return this;
    }

    public String getCodeBarrePack() {
        return codeBarrePack;
    }

    public Promotion setCodeBarrePack(String codeBarrePack) {
        this.codeBarrePack = codeBarrePack;
        return this;
    }

    public Set<Magasin> getMagasins() {
        return magasins;
    }

    public Promotion setMagasins(Set<Magasin> magasins) {
        this.magasins = magasins;
        return this;
    }

    public Set<PromotionProduit> getProduits() {
        return produits;
    }

    public Promotion setProduits(Set<PromotionProduit> produits) {
        this.produits = produits;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public Promotion setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Promotion setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
