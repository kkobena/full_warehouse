package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

/** not an ignored comment */
@Getter
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Entity
@Table(
    name = "produit",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"libelle", "type_produit"})},
    indexes = {
      @Index(columnList = "libelle ASC", name = "libelle_index"),
      @Index(columnList = "code_ean", name = "codeEan_index"),
      @Index(columnList = "status", name = "status_index"),
    })
public class Produit implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter
  @NotNull
  @Column(name = "libelle", nullable = false)
  private String libelle;

  @Getter
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "type_produit", nullable = false)
  private TypeProduit typeProduit;

  @Getter
  @NotNull
  @Column(name = "cost_amount", nullable = false)
  private Integer costAmount;

  @Getter
  @NotNull
  @Column(name = "regular_unit_price", nullable = false)
  private Integer regularUnitPrice;

  @Getter
  @Column(name = "net_unit_price", nullable = false)
  private Integer netUnitPrice;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Getter
  @NotNull
  @Min(value = 0)
  @Column(name = "item_qty", nullable = false)
  private Integer itemQty = 1;

  @Getter
  @Column(name = "qty_appro", columnDefinition = "int default '0'")
  private Integer qtyAppro;

  @Getter
  @Column(name = "qty_seuil_mini", columnDefinition = "int default '0'")
  private Integer qtySeuilMini;

  @Getter
  @Column(name = "check_expiry_date", columnDefinition = "boolean default false")
  private Boolean checkExpiryDate;

  @Getter
  @NotAudited
  @Column(name = "chiffre", columnDefinition = "boolean default true")
  private Boolean chiffre = true;

  @Getter
  @NotNull
  @Min(value = 0)
  @Column(name = "item_cost_amount", nullable = false)
  private Integer itemCostAmount = 0;

  @Getter
  @Column(
      name = "scheduled",
      columnDefinition =
          "boolean default false COMMENT 'pour les produits avec une obligation ordonnance'")
  private Boolean scheduled = false;

  @Getter
  @NotNull
  @Min(value = 0)
  @Column(name = "item_regular_unit_price", nullable = false)
  private Integer itemRegularUnitPrice = 0;

  @Getter
  @NotNull
  @Column(name = "prix_mnp", nullable = false, columnDefinition = "int default '0'")
  private Integer prixMnp = 0;




  @Getter
  @NotNull
  @Column(name = "deconditionnable", nullable = false)
  private Boolean deconditionnable;


  @Getter
  @NotAudited
  @ManyToOne
  @JsonIgnoreProperties(value = "produits", allowSetters = true)
  private Produit parent;

  @Getter
  @NotAudited
  @OneToMany(
      mappedBy = "parent",
      fetch = FetchType.EAGER,
      cascade = {CascadeType.REMOVE})
  private List<Produit> produits = new ArrayList<>();

  @Getter
  @NotAudited
  @OneToMany(
      mappedBy = "produit",
      fetch = FetchType.EAGER,
      cascade = {CascadeType.REMOVE})
  private Set<StockProduit> stockProduits = new HashSet<>();

  @Getter
  @NotAudited
  @ManyToOne(optional = false)
  @JsonIgnoreProperties(value = "produits", allowSetters = true)
  @NotNull
  private Tva tva;

  @Getter
  @ManyToOne
  @JsonIgnoreProperties(value = "produits", allowSetters = true)
  private RemiseProduit remise;

  @Getter
  @NotAudited
  @ManyToOne
  @JsonIgnoreProperties(value = "produits", allowSetters = true)
  private Laboratoire laboratoire;

  @Getter
  @NotAudited
  @ManyToOne
  @JsonIgnoreProperties(value = "produits", allowSetters = true)
  private FormProduit forme;

  @Getter
  @Column(name = "code_ean")
  private String codeEan;

  @Getter
  @NotAudited
  @ManyToOne
  @JsonIgnoreProperties(value = "produits", allowSetters = true)
  private FamilleProduit famille;

  @Getter
  @NotAudited
  @ManyToOne
  @JsonIgnoreProperties(value = "produits", allowSetters = true)
  private GammeProduit gamme;

  @Getter
  @NotAudited
  @ManyToOne
  @JsonIgnoreProperties(value = "produits", allowSetters = true)
  private TypeEtiquette typeEtyquette;

  @Getter
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "status", nullable = false)
  private Status status = Status.ENABLE;

  @Getter
  @Column(name = "perime_at")
  private LocalDate perimeAt;

  @Getter
  @NotAudited
  @OneToMany(
      mappedBy = "produit",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
  private Set<FournisseurProduit> fournisseurProduits = new HashSet<>();

  @Getter
  @NotAudited
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinFormula(
      "(SELECT o.id FROM fournisseur_produit o WHERE o.principal=1 AND o.produit_id=id LIMIT 1)")
  private FournisseurProduit fournisseurProduitPrincipal;

  @Getter
  @NotAudited
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinFormula(
      "(SELECT o.id FROM stock_produit o WHERE  o.storage_id=2 AND o.produit_id=id LIMIT  1)")
  private StockProduit stockProduitPointOfSale;

  @Getter
  @NotAudited
  @OneToMany(
      mappedBy = "produit",
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
  private Set<RayonProduit> rayonProduits = new HashSet<>();

  /* @Type(type = "json")
  @Column(columnDefinition = "json", name = "parcours_json")
  private List<ParcoursProduit> parcoursProduits = new ArrayList<>();*/
  @Getter
  @NotAudited
  @Type(type = "io.hypersistence.utils.hibernate.type.json.JsonType")
  @Column(columnDefinition = "json", name = "daily_stock_json")
  private Set<DailyStock> dailyStocks = new HashSet<>();

  @Getter
  @ManyToOne private Tableau tableau;

  @Getter
  @Min(value = 0)
  @Column(name = "cmu_amount")
  private Integer cmuAmount;


  /*
    public List<ParcoursProduit> getParcoursProduits() {
    return parcoursProduits;
  }

  public Produit setParcoursProduits(List<ParcoursProduit> parcoursProduits) {
    this.parcoursProduits = parcoursProduits;
    return this;
  }*/

  public Produit setTableau(Tableau tableau) {
    this.tableau = tableau;
    return this;
  }

  public Produit setCmuAmount(Integer cmuAmount) {
    this.cmuAmount = cmuAmount;
    return this;
  }

  public Produit setDailyStocks(Set<DailyStock> dailyStocks) {
    this.dailyStocks = dailyStocks;
    return this;
  }

  public Produit setFournisseurProduitPrincipal(FournisseurProduit fournisseurProduitPrincipal) {
    this.fournisseurProduitPrincipal = fournisseurProduitPrincipal;
    return this;
  }

  public void setStockProduits(Set<StockProduit> stockProduits) {
    this.stockProduits = stockProduits;
  }

  public Produit setRayonProduits(Set<RayonProduit> rayonProduits) {
    this.rayonProduits = rayonProduits;
    return this;
  }

  public Produit id(Long id) {
    this.id = id;
    return this;
  }

  public Produit setStockProduitPointOfSale(StockProduit stockProduitPointOfSale) {
    this.stockProduitPointOfSale = stockProduitPointOfSale;
    return this;
  }

  public void setTva(Tva tva) {
    this.tva = tva;
  }

  public Produit setStatus(Status status) {
    this.status = status;
    return this;
  }

  public Produit setFournisseurProduits(Set<FournisseurProduit> fournisseurProduits) {
    this.fournisseurProduits = fournisseurProduits;
    return this;
  }

  public Produit setTypeEtyquette(TypeEtiquette typeEtyquette) {
    this.typeEtyquette = typeEtyquette;
    return this;
  }

  public void setRemise(RemiseProduit remise) {
    this.remise = remise;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setLibelle(String libelle) {
    this.libelle = libelle;
  }

  public Produit libelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public void setTypeProduit(TypeProduit typeProduit) {
    this.typeProduit = typeProduit;
  }

  public Produit typeProduit(TypeProduit typeProduit) {
    this.typeProduit = typeProduit;
    return this;
  }

  public void setCostAmount(Integer costAmount) {
    this.costAmount = costAmount;
  }

  public Produit costAmount(Integer costAmount) {
    this.costAmount = costAmount;
    return this;
  }

  public void setRegularUnitPrice(Integer regularUnitPrice) {
    this.regularUnitPrice = regularUnitPrice;
  }

  public Produit regularUnitPrice(Integer regularUnitPrice) {
    this.regularUnitPrice = regularUnitPrice;
    return this;
  }

  public void setNetUnitPrice(Integer netUnitPrice) {
    this.netUnitPrice = netUnitPrice;
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

  public void setItemQty(Integer itemQty) {
    this.itemQty = itemQty;
  }

  public Produit itemQty(Integer itemQty) {
    this.itemQty = itemQty;
    return this;
  }

  public void setItemCostAmount(Integer itemCostAmount) {
    this.itemCostAmount = itemCostAmount;
  }

  public Produit itemCostAmount(Integer itemCostAmount) {
    this.itemCostAmount = itemCostAmount;
    return this;
  }

  public void setItemRegularUnitPrice(Integer itemRegularUnitPrice) {
    this.itemRegularUnitPrice = itemRegularUnitPrice;
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




  public void setParent(Produit parent) {
    this.parent = parent;
  }

  public void setProduits(List<Produit> produits) {
    this.produits = produits;
  }

  public void setQtyAppro(Integer qtyAppro) {
    this.qtyAppro = qtyAppro;
  }

  public void setQtySeuilMini(Integer qtySeuilMini) {
    this.qtySeuilMini = qtySeuilMini;
  }

  public Produit setCheckExpiryDate(Boolean checkExpiryDate) {
    this.checkExpiryDate = checkExpiryDate;
    return this;
  }

  public void setChiffre(Boolean chiffre) {
    this.chiffre = chiffre;
  }

  public void setPrixMnp(Integer prixMnp) {
    this.prixMnp = prixMnp;
  }

  public void setDeconditionnable(Boolean deconditionnable) {
    this.deconditionnable = deconditionnable;
  }

  public void setLaboratoire(Laboratoire laboratoire) {
    this.laboratoire = laboratoire;
  }

  public void setForme(FormProduit forme) {
    this.forme = forme;
  }

  public void setCodeEan(String codeEan) {
    this.codeEan = codeEan;
  }

  public void setFamille(FamilleProduit famille) {
    this.famille = famille;
  }

  public void setGamme(GammeProduit gamme) {
    this.gamme = gamme;
  }

  public Produit setScheduled(Boolean scheduled) {
    this.scheduled = scheduled;
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

  public Produit setPerimeAt(LocalDate perimeAt) {
    this.perimeAt = perimeAt;
    return this;
  }

  public Produit removeFournisseurProduit(FournisseurProduit fournisseurProduit) {
    fournisseurProduits.remove(fournisseurProduit);
    fournisseurProduit.setProduit(null);
    return this;
  }
}
