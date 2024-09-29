package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.type.SqlTypes;

/**
 * not an ignored comment
 */
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Entity
@Table(
    name = "produit",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "libelle", "type_produit" }) },
    indexes = {
        @Index(columnList = "libelle ASC", name = "libelle_index"),
        @Index(columnList = "code_ean", name = "codeEan_index"),
        @Index(columnList = "status", name = "status_index"),
    }
)
public class Produit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type_produit", nullable = false)
    private TypeProduit typeProduit;

    @NotNull
    @Column(name = "cost_amount", nullable = false)
    private Integer costAmount;

    @NotNull
    @Column(name = "regular_unit_price", nullable = false)
    private Integer regularUnitPrice;

    @Column(name = "net_unit_price", nullable = false)
    private Integer netUnitPrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @NotNull
    @Min(value = 0)
    @Column(name = "item_qty", nullable = false)
    private Integer itemQty = 1;

    @Column(name = "qty_appro", columnDefinition = "int default '0'")
    private Integer qtyAppro;

    @Column(name = "qty_seuil_mini", columnDefinition = "int default '0'")
    private Integer qtySeuilMini;

    @Column(name = "check_expiry_date", columnDefinition = "boolean default false")
    private Boolean checkExpiryDate;

    @NotAudited
    @Column(name = "chiffre", columnDefinition = "boolean default true")
    private Boolean chiffre = true;

    @NotNull
    @Min(value = 0)
    @Column(name = "item_cost_amount", nullable = false)
    private Integer itemCostAmount = 0;

    @Column(name = "scheduled", columnDefinition = "boolean default false COMMENT 'pour les produits avec une obligation ordonnance'")
    private Boolean scheduled = false;

    @NotNull
    @Min(value = 0)
    @Column(name = "item_regular_unit_price", nullable = false)
    private Integer itemRegularUnitPrice = 0;

    @Min(value = 0)
    @Column(name = "prix_reference")
    private Integer PrixRererence;

    @NotNull
    @Column(name = "prix_mnp", nullable = false, columnDefinition = "int default '0'")
    private Integer prixMnp = 0;

    @NotNull
    @Column(name = "deconditionnable", nullable = false)
    private Boolean deconditionnable;

    @NotAudited
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private Produit parent;

    @NotAudited
    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
    private List<Produit> produits = new ArrayList<>();

    @NotAudited
    @OneToMany(mappedBy = "produit", fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
    private Set<StockProduit> stockProduits = new HashSet<>();

    @NotAudited
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    @NotNull
    private Tva tva;

    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private RemiseProduit remise;

    @NotAudited
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private Laboratoire laboratoire;

    @NotAudited
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private FormProduit forme;

    @Column(name = "code_ean")
    private String codeEan;

    @NotNull
    @NotAudited
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private FamilleProduit famille;

    @NotAudited
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private GammeProduit gamme;

    @NotAudited
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private TypeEtiquette typeEtyquette;

    @NotAudited
    @ManyToOne
    private Dci dci;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private Status status = Status.ENABLE;

    @Column(name = "perime_at")
    private LocalDate perimeAt;

    @NotAudited
    @OneToMany(mappedBy = "produit", cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    private Set<FournisseurProduit> fournisseurProduits = new HashSet<>();

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinFormula("(SELECT o.id FROM fournisseur_produit o WHERE o.principal=1 AND o.produit_id=id LIMIT 1)")
    private FournisseurProduit fournisseurProduitPrincipal;

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinFormula("(SELECT o.id FROM stock_produit o WHERE  o.storage_id=2 AND o.produit_id=id LIMIT  1)")
    private StockProduit stockProduitPointOfSale;

    @NotAudited
    @OneToMany(mappedBy = "produit", fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    private Set<RayonProduit> rayonProduits = new HashSet<>();

    /* @Type(type = "json")
  @Column(columnDefinition = "json", name = "parcours_json")
  private List<ParcoursProduit> parcoursProduits = new ArrayList<>();*/

    @NotAudited
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", name = "daily_stock_json")
    private Set<DailyStock> dailyStocks = new HashSet<>();

    @ManyToOne
    private Tableau tableau;

    @Min(value = 0)
    @Column(name = "cmu_amount", columnDefinition = "int default '0'")
    private Integer cmuAmount = 0;

    /*
    seuil minimun en point de vente pour declencher un reassort
     */
    @Min(value = 0)
    @Column(name = "seuil_reassort")
    private Integer seuilReassort;

    /*
    seuil minimun du detail en point de vente pour declencher un deconditionnement
     */
    @Min(value = 0)
    @Column(name = "seuil_decond")
    private Integer seuilDeconditionnement;

    @Column(name = "remisable", columnDefinition = "boolean default true")
    private Boolean remisable = Boolean.TRUE;

    public @Min(value = 0) Integer getSeuilReassort() {
        return seuilReassort;
    }

    public void setSeuilReassort(@Min(value = 0) Integer seuilReassort) {
        this.seuilReassort = seuilReassort;
    }

    public @Min(value = 0) Integer getSeuilDeconditionnement() {
        return seuilDeconditionnement;
    }

    public void setSeuilDeconditionnement(@Min(value = 0) Integer seuilDeconditionnement) {
        this.seuilDeconditionnement = seuilDeconditionnement;
    }

    public Boolean getRemisable() {
        return remisable;
    }

    public void setRemisable(Boolean remisable) {
        this.remisable = remisable;
    }

    /*
    public List<ParcoursProduit> getParcoursProduits() {
    return parcoursProduits;
  }

  public Produit setParcoursProduits(List<ParcoursProduit> parcoursProduits) {
    this.parcoursProduits = parcoursProduits;
    return this;
  }*/

    public @Min(value = 0) Integer getPrixRererence() {
        return PrixRererence;
    }

    public void setPrixRererence(@Min(value = 0) Integer prixRererence) {
        PrixRererence = prixRererence;
    }

    public Dci getDci() {
        return dci;
    }

    public void setDci(Dci dci) {
        this.dci = dci;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public TypeProduit getTypeProduit() {
        return typeProduit;
    }

    public void setTypeProduit(TypeProduit typeProduit) {
        this.typeProduit = typeProduit;
    }

    public @NotNull Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public @NotNull Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public void setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

    public Integer getNetUnitPrice() {
        return netUnitPrice;
    }

    public void setNetUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
    }

    public @NotNull @Min(value = 0) Integer getItemQty() {
        return itemQty;
    }

    public void setItemQty(Integer itemQty) {
        this.itemQty = itemQty;
    }

    public Integer getQtyAppro() {
        return qtyAppro;
    }

    public void setQtyAppro(Integer qtyAppro) {
        this.qtyAppro = qtyAppro;
    }

    public Integer getQtySeuilMini() {
        return qtySeuilMini;
    }

    public void setQtySeuilMini(Integer qtySeuilMini) {
        this.qtySeuilMini = qtySeuilMini;
    }

    public Boolean getCheckExpiryDate() {
        return checkExpiryDate;
    }

    public Produit setCheckExpiryDate(Boolean checkExpiryDate) {
        this.checkExpiryDate = checkExpiryDate;
        return this;
    }

    public Boolean getChiffre() {
        return chiffre;
    }

    public void setChiffre(Boolean chiffre) {
        this.chiffre = chiffre;
    }

    public @NotNull @Min(value = 0) Integer getItemCostAmount() {
        return itemCostAmount;
    }

    public void setItemCostAmount(Integer itemCostAmount) {
        this.itemCostAmount = itemCostAmount;
    }

    public Boolean getScheduled() {
        return scheduled;
    }

    public Produit setScheduled(Boolean scheduled) {
        this.scheduled = scheduled;
        return this;
    }

    public @NotNull @Min(value = 0) Integer getItemRegularUnitPrice() {
        return itemRegularUnitPrice;
    }

    public void setItemRegularUnitPrice(Integer itemRegularUnitPrice) {
        this.itemRegularUnitPrice = itemRegularUnitPrice;
    }

    public @NotNull Integer getPrixMnp() {
        return prixMnp;
    }

    public void setPrixMnp(Integer prixMnp) {
        this.prixMnp = prixMnp;
    }

    public @NotNull Boolean getDeconditionnable() {
        return deconditionnable;
    }

    public void setDeconditionnable(Boolean deconditionnable) {
        this.deconditionnable = deconditionnable;
    }

    public Produit getParent() {
        return parent;
    }

    public void setParent(Produit parent) {
        this.parent = parent;
    }

    public List<Produit> getProduits() {
        return produits;
    }

    public void setProduits(List<Produit> produits) {
        this.produits = produits;
    }

    public Set<StockProduit> getStockProduits() {
        return stockProduits;
    }

    public void setStockProduits(Set<StockProduit> stockProduits) {
        this.stockProduits = stockProduits;
    }

    public @NotNull Tva getTva() {
        return tva;
    }

    public void setTva(Tva tva) {
        this.tva = tva;
    }

    public RemiseProduit getRemise() {
        return remise;
    }

    public void setRemise(RemiseProduit remise) {
        this.remise = remise;
    }

    public Laboratoire getLaboratoire() {
        return laboratoire;
    }

    public void setLaboratoire(Laboratoire laboratoire) {
        this.laboratoire = laboratoire;
    }

    public FormProduit getForme() {
        return forme;
    }

    public void setForme(FormProduit forme) {
        this.forme = forme;
    }

    public String getCodeEan() {
        return codeEan;
    }

    public void setCodeEan(String codeEan) {
        this.codeEan = codeEan;
    }

    public FamilleProduit getFamille() {
        return famille;
    }

    public void setFamille(FamilleProduit famille) {
        this.famille = famille;
    }

    public GammeProduit getGamme() {
        return gamme;
    }

    public void setGamme(GammeProduit gamme) {
        this.gamme = gamme;
    }

    public TypeEtiquette getTypeEtyquette() {
        return typeEtyquette;
    }

    public Produit setTypeEtyquette(TypeEtiquette typeEtyquette) {
        this.typeEtyquette = typeEtyquette;
        return this;
    }

    public @NotNull Status getStatus() {
        return status;
    }

    public Produit setStatus(Status status) {
        this.status = status;
        return this;
    }

    public LocalDate getPerimeAt() {
        return perimeAt;
    }

    public Produit setPerimeAt(LocalDate perimeAt) {
        this.perimeAt = perimeAt;
        return this;
    }

    public Set<FournisseurProduit> getFournisseurProduits() {
        return fournisseurProduits;
    }

    public Produit setFournisseurProduits(Set<FournisseurProduit> fournisseurProduits) {
        this.fournisseurProduits = fournisseurProduits;
        return this;
    }

    public FournisseurProduit getFournisseurProduitPrincipal() {
        return fournisseurProduitPrincipal;
    }

    public Produit setFournisseurProduitPrincipal(FournisseurProduit fournisseurProduitPrincipal) {
        this.fournisseurProduitPrincipal = fournisseurProduitPrincipal;
        return this;
    }

    public StockProduit getStockProduitPointOfSale() {
        return stockProduitPointOfSale;
    }

    public Produit setStockProduitPointOfSale(StockProduit stockProduitPointOfSale) {
        this.stockProduitPointOfSale = stockProduitPointOfSale;
        return this;
    }

    public Set<RayonProduit> getRayonProduits() {
        return rayonProduits;
    }

    public Produit setRayonProduits(Set<RayonProduit> rayonProduits) {
        this.rayonProduits = rayonProduits;
        return this;
    }

    public Set<DailyStock> getDailyStocks() {
        return dailyStocks;
    }

    public Produit setDailyStocks(Set<DailyStock> dailyStocks) {
        this.dailyStocks = dailyStocks;
        return this;
    }

    public Tableau getTableau() {
        return tableau;
    }

    public Produit setTableau(Tableau tableau) {
        this.tableau = tableau;
        return this;
    }

    public @Min(value = 0) Integer getCmuAmount() {
        return cmuAmount;
    }

    public Produit setCmuAmount(Integer cmuAmount) {
        this.cmuAmount = cmuAmount;
        return this;
    }

    public Produit id(Long id) {
        this.id = id;
        return this;
    }

    public Produit libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public Produit typeProduit(TypeProduit typeProduit) {
        this.typeProduit = typeProduit;
        return this;
    }

    public Produit costAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public Produit regularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
        return this;
    }

    public Produit netUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Produit createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Produit updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Produit itemQty(Integer itemQty) {
        this.itemQty = itemQty;
        return this;
    }

    public Produit itemCostAmount(Integer itemCostAmount) {
        this.itemCostAmount = itemCostAmount;
        return this;
    }

    public Produit itemRegularUnitPrice(Integer itemRegularUnitPrice) {
        this.itemRegularUnitPrice = itemRegularUnitPrice;
        return this;
    }

    public Produit addProduit(Produit produit) {
        produits.add(produit);
        produit.setParent(this);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Produit)) {
            return false;
        }
        return id != null && id.equals(((Produit) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    public Produit addStockProduit(StockProduit stockProduit) {
        stockProduits.add(stockProduit);
        stockProduit.setProduit(this);
        return this;
    }

    public Produit removeStockProduit(StockProduit stockProduit) {
        stockProduits.remove(stockProduit);
        stockProduit.setProduit(null);
        return this;
    }

    public Produit addFournisseurProduit(FournisseurProduit fournisseurProduit) {
        fournisseurProduits.add(fournisseurProduit);
        fournisseurProduit.setProduit(this);
        return this;
    }

    public Produit removeFournisseurProduit(FournisseurProduit fournisseurProduit) {
        fournisseurProduits.remove(fournisseurProduit);
        fournisseurProduit.setProduit(null);
        return this;
    }
}
