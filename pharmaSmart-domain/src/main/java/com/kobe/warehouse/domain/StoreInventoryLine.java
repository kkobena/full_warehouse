package com.kobe.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A StoreInventoryLine.
 */

@NamedStoredProcedureQuery(
    name = "StoreInventoryLine.proc_close_inventory",
    procedureName = "proc_close_inventory",
    parameters = {
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "store_inventory_id", type = Long.class),
        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "nombreLigne", type = Integer.class),
    }
)
@Entity
@Table(name = "store_inventory_line", uniqueConstraints = {
    @UniqueConstraint(name = "uq_sil_produit_inventory_storage", columnNames = {"produit_id",
        "store_inventory_id", "storage_id"})
})
public class StoreInventoryLine implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Verrou optimiste — incrémenté par Hibernate à chaque modification. Les clients
     * renvoient la version lue ; une version périmée signale un comptage concurrent.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "quantity_on_hand")
    private Integer quantityOnHand;

    @Column(name = "gap")
    private Integer gap = 0;

    @Column(name = "quantity_init")
    private Integer quantityInit;

    @Column(name = "quantity_sold")
    private Integer quantitySold = 0;

    @Column(name = "inventory_value_cost")
    private Integer inventoryValueCost;

    @ManyToOne(optional = false)
    @JoinColumn(name = "store_inventory_id", referencedColumnName = "id")
    private StoreInventory storeInventory;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(optional = false)
    private Produit produit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_id")
    private Storage storage;

    @OneToMany(mappedBy = "storeInventoryLine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryLot> lots = new ArrayList<>();

    @NotNull
    @Column(name = "updated", nullable = false)
    private Boolean updated = false;

    /**
     * Utilisateur ayant effectué le dernier comptage de cette ligne — null tant que la
     * ligne n'a pas été comptée. La date correspondante est {@link #updatedAt}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counted_by_id")
    private AppUser countedBy;

    @Column(name = "last_unit_price")
    private Integer lastUnitPrice;

    /**
     * Applique un comptage à la ligne : quantités, écart, valorisation figée au moment du
     * comptage et traçabilité.
     *
     * <p>Centralisé ici parce que trois chemins comptent une ligne — saisie écran et les deux
     * imports CSV — et qu'ils doivent renseigner exactement les mêmes colonnes. Les imports
     * ne posaient que {@code quantityOnHand} et {@code updated} ; {@code inventory_value_cost},
     * {@code last_unit_price} et {@code quantity_init} restaient nuls, ce qui faisait échouer
     * la clôture (leurs contreparties dans {@code inventory_transaction} sont NOT NULL) et
     * sortait la ligne des filtres d'écart, {@code quantity_on_hand <> quantity_init} valant
     * NULL.
     *
     * <p>La valorisation vient du fournisseur principal, avec repli sur le produit — dont
     * {@code costAmount} et {@code regularUnitPrice} sont NOT NULL en base.
     */
    public StoreInventoryLine applyCount(int quantityInit, int quantityOnHand, AppUser countedBy) {
        FournisseurProduit principal = this.produit != null ? this.produit.getFournisseurProduitPrincipal() : null;
        this.quantityInit = quantityInit;
        this.quantityOnHand = quantityOnHand;
        this.gap = quantityOnHand - quantityInit;
        this.quantitySold = 0;
        this.inventoryValueCost = principal != null ? principal.getPrixAchat() : this.produit.getCostAmount();
        this.lastUnitPrice = principal != null ? principal.getPrixUni() : this.produit.getRegularUnitPrice();
        this.updated = true;
        this.updatedAt = LocalDateTime.now();
        this.countedBy = countedBy;
        return this;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public AppUser getCountedBy() {
        return countedBy;
    }

    public StoreInventoryLine setCountedBy(AppUser countedBy) {
        this.countedBy = countedBy;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLastUnitPrice() {
        return lastUnitPrice;
    }

    public void setLastUnitPrice(Integer lastUnitPrice) {
        this.lastUnitPrice = lastUnitPrice;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public StoreInventoryLine quantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public StoreInventoryLine setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Integer getQuantityInit() {
        return quantityInit;
    }

    public void setQuantityInit(Integer quantityInit) {
        this.quantityInit = quantityInit;
    }

    public StoreInventoryLine quantityInit(Integer quantityInit) {
        this.quantityInit = quantityInit;
        return this;
    }

    public Integer getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }

    public StoreInventoryLine quantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
        return this;
    }

    public Integer getInventoryValueCost() {
        return inventoryValueCost;
    }

    public void setInventoryValueCost(Integer inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
    }

    public StoreInventoryLine inventoryValueCost(Integer inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
        return this;
    }

    public StoreInventory getStoreInventory() {
        return storeInventory;
    }

    public void setStoreInventory(StoreInventory storeInventory) {
        this.storeInventory = storeInventory;
    }

    public StoreInventoryLine storeInventory(StoreInventory storeInventory) {
        this.storeInventory = storeInventory;
        return this;
    }

    public Integer getGap() {
        return gap;
    }

    public void setGap(Integer gap) {
        this.gap = gap;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public StoreInventoryLine produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public Boolean getUpdated() {
        return updated;
    }

    public void setUpdated(Boolean updated) {
        this.updated = updated;
    }

    public Storage getStorage() {
        return storage;
    }

    public StoreInventoryLine setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

    public List<InventoryLot> getLots() {
        return lots;
    }

    public void setLots(List<InventoryLot> lots) {
        this.lots = lots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StoreInventoryLine)) {
            return false;
        }
        return id != null && id.equals(((StoreInventoryLine) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return (
            "StoreInventoryLine{" +
                "id=" +
                getId() +
                ", quantityOnHand=" +
                getQuantityOnHand() +
                ", quantityInit=" +
                getQuantityInit() +
                ", quantitySold=" +
                getQuantitySold() +
                ", inventoryValueCost=" +
                getInventoryValueCost() +
                ", inventoryValueLatestSellingPrice=" +
                "}"
        );
    }
}
