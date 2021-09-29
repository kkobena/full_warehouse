package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;

import io.swagger.annotations.ApiModel;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JoinFormula;

/**
 * not an ignored comment
 */
@ApiModel(description = "not an ignored comment")
@Entity
@Table(name = "produit", uniqueConstraints = {@UniqueConstraint(columnNames = {"libelle", "type_produit"})},
    indexes = {
        @Index(columnList = "libelle ASC", name = "libelle_index"),
        @Index(columnList = "code_ean", name = "codeEan_index"),
        @Index(columnList = "status", name = "status_index")

    }
)
public class Produit implements Serializable {
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
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @NotNull
    @Min(value = 0)
    @Column(name = "item_qty", nullable = false)
    private Integer itemQty=1;
    @Column(name = "qty_appro", columnDefinition = "int default '0'")
    private Integer qtyAppro;
    @Column(name = "qty_seuil_mini", columnDefinition = "int default '0'")
    private Integer qtySeuilMini;
    @Column(name = "dateperemption", columnDefinition = "boolean default false")
    private Boolean dateperemption;
    @Column(name = "chiffre", columnDefinition = "boolean default true")
    private Boolean chiffre=true;
    @NotNull
    @Min(value = 0)
    @Column(name = "item_cost_amount", nullable = false)
    private Integer itemCostAmount=0;
    @Column(name = "scheduled", columnDefinition = "boolean default false COMMENT 'pour les produits avec une obligation ordonnance'")
    private Boolean scheduled=false;
    @NotNull
    @Min(value = 0)
    @Column(name = "item_regular_unit_price", nullable = false)
    private Integer itemRegularUnitPrice=0;
    @NotNull
    @Column(name = "prix_mnp", nullable = false, columnDefinition = "int default '0'")
    private Integer prixMnp=0;
    @OneToMany(mappedBy = "produit")
    private Set<SalesLine> salesLines = new HashSet<>();
    @NotNull
    @Column(name = "deconditionnable", nullable = false)
    private Boolean deconditionnable;
    @OneToMany(mappedBy = "produit")
    private Set<StoreInventoryLine> storeInventoryLines = new HashSet<>();
    @OneToMany(mappedBy = "produit")
    private Set<OrderLine> orderLines = new HashSet<>();
    @OneToMany(mappedBy = "produit")
    private Set<InventoryTransaction> inventoryTransactions = new HashSet<>();
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private Produit parent;
    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE})
    private List<Produit> produits = new ArrayList<>();
    @OneToMany(mappedBy = "produit", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE})
    private Set<StockProduit> stockProduits = new HashSet<>();
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    @NotNull
    private Tva tva;
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private RemiseProduit remise;
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private Laboratoire laboratoire;
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private FormProduit forme;
    @Column(name = "code_ean")
    private String codeEan;
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private FamilleProduit famille;
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private GammeProduit gamme;
    @ManyToOne
    @JsonIgnoreProperties(value = "produits", allowSetters = true)
    private TypeEtiquette typeEtyquette;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private Status status = Status.ENABLE;
    @Column(name = "perime_at")
    private LocalDate perimeAt;
    public Set<StockProduit> getStockProduits() {
        return stockProduits;
    }
    @OneToMany(mappedBy = "produit", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    private Set<FournisseurProduit> fournisseurProduits = new HashSet<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinFormula("(SELECT o.id FROM fournisseur_produit o WHERE o.principal=1 AND o.produit_id=id LIMIT 1)")
    private FournisseurProduit fournisseurProduitPrincipal;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinFormula("(SELECT o.id FROM stock_produit o,rayon r WHERE o.rayon_id=r.id AND r.storage_id=2 AND o.produit_id=id LIMIT  1)")
    private StockProduit stockProduitPointOfSale;

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

    public void setStockProduits(Set<StockProduit> stockProduits) {
        this.stockProduits = stockProduits;
    }

    public Tva getTva() {
        return tva;
    }

    public Status getStatus() {
        return status;
    }

    public Produit setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Set<FournisseurProduit> getFournisseurProduits() {
        return fournisseurProduits;
    }

    public Produit setFournisseurProduits(Set<FournisseurProduit> fournisseurProduits) {
        this.fournisseurProduits = fournisseurProduits;
        return this;
    }

    public TypeEtiquette getTypeEtyquette() {
        return typeEtyquette;
    }

    public Produit setTypeEtyquette(TypeEtiquette typeEtyquette) {
        this.typeEtyquette = typeEtyquette;
        return this;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public Produit libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public TypeProduit getTypeProduit() {
        return typeProduit;
    }

    public Produit typeProduit(TypeProduit typeProduit) {
        this.typeProduit = typeProduit;
        return this;
    }

    public void setTypeProduit(TypeProduit typeProduit) {
        this.typeProduit = typeProduit;
    }


    public Integer getCostAmount() {
        return costAmount;
    }

    public Produit costAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public Produit regularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
        return this;
    }

    public void setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

    public Integer getNetUnitPrice() {
        return netUnitPrice;
    }

    public Produit netUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
        return this;
    }

    public void setNetUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
    }

    public Instant getCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        return createdAt;
    }

    public Produit createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        return updatedAt;
    }

    public Produit updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getItemQty() {
        return itemQty;
    }

    public Produit itemQty(Integer itemQty) {
        this.itemQty = itemQty;
        return this;
    }

    public void setItemQty(Integer itemQty) {
        this.itemQty = itemQty;
    }

    public Integer getItemCostAmount() {
        return itemCostAmount;
    }

    public Produit itemCostAmount(Integer itemCostAmount) {
        this.itemCostAmount = itemCostAmount;
        return this;
    }

    public void setItemCostAmount(Integer itemCostAmount) {
        this.itemCostAmount = itemCostAmount;
    }

    public Integer getItemRegularUnitPrice() {
        return itemRegularUnitPrice;
    }

    public Produit itemRegularUnitPrice(Integer itemRegularUnitPrice) {
        this.itemRegularUnitPrice = itemRegularUnitPrice;
        return this;
    }

    public void setItemRegularUnitPrice(Integer itemRegularUnitPrice) {
        this.itemRegularUnitPrice = itemRegularUnitPrice;
    }

    public Set<SalesLine> getSalesLines() {
        return salesLines;
    }

    public Produit salesLines(Set<SalesLine> salesLines) {
        this.salesLines = salesLines;
        return this;
    }

    public Produit addSalesLine(SalesLine salesLine) {
        this.salesLines.add(salesLine);
        salesLine.setProduit(this);
        return this;
    }


    public void setSalesLines(Set<SalesLine> salesLines) {
        this.salesLines = salesLines;
    }

    public Set<StoreInventoryLine> getStoreInventoryLines() {
        return storeInventoryLines;
    }

    public Produit storeInventoryLines(Set<StoreInventoryLine> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
        return this;
    }

    public Produit addStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
        this.storeInventoryLines.add(storeInventoryLine);
        storeInventoryLine.setProduit(this);
        return this;
    }

    public Produit removeStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
        this.storeInventoryLines.remove(storeInventoryLine);
        storeInventoryLine.setProduit(null);
        return this;
    }

    public void setStoreInventoryLines(Set<StoreInventoryLine> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
    }

    public Set<OrderLine> getOrderLines() {
        return orderLines;
    }

    public Produit orderLines(Set<OrderLine> orderLines) {
        this.orderLines = orderLines;
        return this;
    }

    public Produit addOrderLine(OrderLine orderLine) {
        this.orderLines.add(orderLine);
        orderLine.setProduit(this);
        return this;
    }

    public Produit addProduit(Produit produit) {
        this.produits.add(produit);
        produit.setParent(this);
        return this;
    }

    public void setOrderLines(Set<OrderLine> orderLines) {
        this.orderLines = orderLines;
    }

    public Set<InventoryTransaction> getInventoryTransactions() {
        return inventoryTransactions;
    }

    public Produit inventoryTransactions(Set<InventoryTransaction> inventoryTransactions) {
        this.inventoryTransactions = inventoryTransactions;
        return this;
    }

    public Produit addInventoryTransaction(InventoryTransaction inventoryTransaction) {
        this.inventoryTransactions.add(inventoryTransaction);
        inventoryTransaction.setProduit(this);
        return this;
    }

    public Produit removeInventoryTransaction(InventoryTransaction inventoryTransaction) {
        this.inventoryTransactions.remove(inventoryTransaction);
        inventoryTransaction.setProduit(null);
        return this;
    }

    public void setInventoryTransactions(Set<InventoryTransaction> inventoryTransactions) {
        this.inventoryTransactions = inventoryTransactions;
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

    public static Produit detailFromParent(Produit parent) {
        Produit produit = new Produit();
        produit.setParent(parent);
        produit.setLibelle(parent.getLibelle());
        produit.setUpdatedAt(parent.getUpdatedAt());
        produit.setCreatedAt(parent.getCreatedAt());
        produit.setCostAmount(parent.getItemCostAmount());
        produit.setItemQty(0);
        produit.setItemRegularUnitPrice(0);
        produit.setItemCostAmount(0);
        produit.setRegularUnitPrice(parent.getItemRegularUnitPrice());
        produit.setNetUnitPrice(0);
        produit.setTypeProduit(TypeProduit.DETAIL);

        return produit;
    }

    public static Produit detailFromParent(Produit parent, Produit produit) {
        produit.setUpdatedAt(parent.getUpdatedAt());
        produit.setCostAmount(parent.getItemCostAmount());
        return produit;
    }

    public void setProduits(List<Produit> produits) {
        this.produits = produits;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
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

    public Boolean getDateperemption() {
        return dateperemption;
    }

    public void setDateperemption(Boolean dateperemption) {
        this.dateperemption = dateperemption;
    }

    public Boolean getChiffre() {
        return chiffre;
    }

    public void setChiffre(Boolean chiffre) {
        this.chiffre = chiffre;
    }


    public Integer getPrixMnp() {
        return prixMnp;
    }

    public void setPrixMnp(Integer prixMnp) {
        this.prixMnp = prixMnp;
    }

    public Boolean getDeconditionnable() {
        return deconditionnable;
    }

    public void setDeconditionnable(Boolean deconditionnable) {
        this.deconditionnable = deconditionnable;
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

    public Boolean getScheduled() {
        return scheduled;
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
        this.stockProduits.add(stockProduit);
        stockProduit.setProduit(this);
        return this;
    }

    public Produit removeStockProduit(StockProduit stockProduit) {
        this.stockProduits.remove(stockProduit);
        stockProduit.setProduit(null);
        return this;
    }

    public Produit addFournisseurProduit(FournisseurProduit fournisseurProduit) {
        this.fournisseurProduits.add(fournisseurProduit);
        fournisseurProduit.setProduit(this);
        return this;
    }

    public LocalDate getPerimeAt() {
        return perimeAt;
    }

    public Produit setPerimeAt(LocalDate perimeAt) {
        this.perimeAt = perimeAt;
        return this;
    }

    public Produit removeFournisseurProduit(FournisseurProduit fournisseurProduit) {
        this.fournisseurProduits.remove(fournisseurProduit);
        fournisseurProduit.setProduit(null);
        return this;
    }

}
