package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.kobe.warehouse.domain.DailyStock;
import com.kobe.warehouse.domain.ParcoursProduit;
import com.kobe.warehouse.domain.Tableau;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProduitDTO {

  @Default private String codeCip = "";
  private Long id;
  private String libelle;
  private int itemQuantity;
  private int qtyUG;
  private TypeProduit typeProduit;
  private Integer quantity;
  private Integer costAmount;
  private Integer regularUnitPrice;
  private Integer netUnitPrice;
  private Instant createdAt;
  private Instant updatedAt;
  private Integer itemQty;
  private Integer itemCostAmount;
  private Integer itemRegularUnitPrice;
  private Long produitId;
  private String produitLibelle;
  private int quantityReceived;
  @Singular private List<ProduitDTO> produits;
  private Instant lastDateOfSale;
  private Instant lastOrderDate;
  private Instant lastInventoryDate;
  @Default private Integer prixMnp = 0;
  private Long parentId;
  private Long fournisseurId;
  private String parentLibelle;
  private Long laboratoireId;
  private String laboratoireLibelle;
  private Long formeId;
  private String formeLibelle;
  private Long typeEtiquetteId;
  private String typeEtiquetteLibelle;
  private Long familleId;
  private String familleLibelle;
  private Long gammeId;
  private String gammeLibelle;
  private Long tvaId;
  private Integer tvaTaux;
  @Singular private Set<StockProduitDTO> stockProduits;
  @Singular private Set<FournisseurProduitDTO> fournisseurProduits;
  private StockProduitDTO stockProduit;
  private FournisseurProduitDTO fournisseurProduit;
  private String qtyStatus;
  @Default private Integer qtyAppro = 0;
  @Default private Integer qtySeuilMini = 0;
  @Default private Boolean dateperemption = false;
  @Default private Boolean chiffre = true;
  private int totalQuantity;
  private int qtyReserve;
  @Default private Boolean deconditionnable = false;
  private String codeEan;
  private String rayonLibelle;
  private Long remiseId;
  private long rayonId;
  private long storageId;
  private float tauxRemise;
  private Integer cmuAmount;
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate perimeAt;
  private int status;
  private String displayStatut;
  private int saleOfPointStock;
  private int saleOfPointVirtualStock;
  private String expirationDate;
  private String displayField;
  @Singular private List<RayonProduitDTO> rayonProduits;
  @Singular private List<ParcoursProduit> parcoursProduits;
  @Singular private List<DailyStock> dailyStocks;
  private Tableau tableau;

    public Tableau getTableau() {
        return tableau;
    }

    public ProduitDTO setTableau(Tableau tableau) {
        this.tableau = tableau;
        return this;
    }

    public ProduitDTO setSaleOfPointStock(int saleOfPointStock) {
    this.saleOfPointStock = saleOfPointStock;
    return this;
  }

  public ProduitDTO displayStatut(String displayStatut) {
    this.displayStatut = displayStatut;
    return this;
  }

  public ProduitDTO setStorageId(Long storageId) {
    this.storageId = storageId;
    return this;
  }

  public ProduitDTO laboratoireId(Long laboratoireId) {
    this.laboratoireId = laboratoireId;
    return this;
  }

  public ProduitDTO laboratoireLibelle(String laboratoireLibelle) {
    this.laboratoireLibelle = laboratoireLibelle;
    return this;
  }

  public ProduitDTO formeId(Long formeId) {
    this.formeId = formeId;
    return this;
  }

  public ProduitDTO typeEtiquetteId(Long typeEtiquetteId) {
    this.typeEtiquetteId = typeEtiquetteId;
    return this;
  }

  public ProduitDTO familleId(Long familleId) {
    this.familleId = familleId;
    return this;
  }

  public ProduitDTO gammeId(Long gammeId) {
    this.gammeId = gammeId;
    return this;
  }

  public ProduitDTO tvaId(Long tvaId) {
    this.tvaId = tvaId;
    return this;
  }

  public ProduitDTO tvaTaux(Integer tvaTaux) {
    this.tvaTaux = tvaTaux;
    return this;
  }

  public ProduitDTO typeEtiquetteLibelle(String typeEtiquetteLibelle) {
    this.typeEtiquetteLibelle = typeEtiquetteLibelle;
    return this;
  }

  public ProduitDTO gammeLibelle(String gammeLibelle) {
    this.gammeLibelle = gammeLibelle;
    return this;
  }

  public ProduitDTO familleLibelle(String familleLibelle) {
    this.familleLibelle = familleLibelle;
    return this;
  }

  public ProduitDTO expirationDate(String expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }

  public ProduitDTO formeLibelle(String formeLibelle) {
    this.formeLibelle = formeLibelle;
    return this;
  }

  public ProduitDTO setStorageId(long storageId) {
    this.storageId = storageId;
    return this;
  }

  public ProduitDTO setSaleOfPointVirtualStock(int saleOfPointVirtualStock) {
    this.saleOfPointVirtualStock = saleOfPointVirtualStock;
    return this;
  }

  public ProduitDTO setFournisseurId(Long fournisseurId) {
    this.fournisseurId = fournisseurId;
    return this;
  }

  public ProduitDTO setLibelle(String libelle) {
    this.libelle = libelle;
    return this;
  }

  public ProduitDTO setTypeProduit(TypeProduit typeProduit) {
    this.typeProduit = typeProduit;
    return this;
  }

  public ProduitDTO setQuantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  public ProduitDTO setCostAmount(Integer costAmount) {
    this.costAmount = costAmount;
    return this;
  }

  public ProduitDTO setRegularUnitPrice(Integer regularUnitPrice) {
    this.regularUnitPrice = regularUnitPrice;
    return this;
  }

  public ProduitDTO setNetUnitPrice(Integer netUnitPrice) {
    this.netUnitPrice = netUnitPrice;
    return this;
  }

  public ProduitDTO setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public ProduitDTO setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public ProduitDTO setItemQty(Integer itemQty) {
    this.itemQty = itemQty;
    return this;
  }

  public ProduitDTO setItemCostAmount(Integer itemCostAmount) {
    this.itemCostAmount = itemCostAmount;
    return this;
  }

  public ProduitDTO setItemRegularUnitPrice(Integer itemRegularUnitPrice) {
    this.itemRegularUnitPrice = itemRegularUnitPrice;
    return this;
  }

  public ProduitDTO setProduitId(Long produitId) {
    this.produitId = produitId;
    return this;
  }

  public ProduitDTO setProduitLibelle(String produitLibelle) {
    this.produitLibelle = produitLibelle;
    return this;
  }

  public ProduitDTO setQuantityReceived(int quantityReceived) {
    this.quantityReceived = quantityReceived;
    return this;
  }

  public ProduitDTO setProduits(List<ProduitDTO> produits) {
    this.produits = produits;
    return this;
  }

  public ProduitDTO setLastDateOfSale(Instant lastDateOfSale) {
    this.lastDateOfSale = lastDateOfSale;
    return this;
  }

  public ProduitDTO setLastOrderDate(Instant lastOrderDate) {
    this.lastOrderDate = lastOrderDate;
    return this;
  }

  public ProduitDTO setLastInventoryDate(Instant lastInventoryDate) {
    this.lastInventoryDate = lastInventoryDate;
    return this;
  }

  public ProduitDTO setCodeCip(String codeCip) {
    this.codeCip = codeCip;
    return this;
  }

  public ProduitDTO setStockProduit(StockProduitDTO stockProduit) {
    this.stockProduit = stockProduit;
    return this;
  }

  public ProduitDTO setFournisseurProduit(FournisseurProduitDTO fournisseurProduit) {
    this.fournisseurProduit = fournisseurProduit;
    return this;
  }

  public ProduitDTO setQtyAppro(Integer qtyAppro) {
    this.qtyAppro = qtyAppro;
    return this;
  }

  public ProduitDTO setQtySeuilMini(Integer qtySeuilMini) {
    this.qtySeuilMini = qtySeuilMini;
    return this;
  }

  public ProduitDTO setTotalQuantity(int totalQuantity) {
    this.totalQuantity = totalQuantity;
    return this;
  }

  public ProduitDTO setDeconditionnable(Boolean deconditionnable) {
    this.deconditionnable = deconditionnable;
    return this;
  }

  public ProduitDTO setCodeEan(String codeEan) {
    this.codeEan = codeEan;
    return this;
  }

  public ProduitDTO setRayonLibelle(String rayonLibelle) {
    this.rayonLibelle = rayonLibelle;
    return this;
  }

  public ProduitDTO setRemiseId(Long remiseId) {
    this.remiseId = remiseId;
    return this;
  }

  public ProduitDTO setRayonId(Long rayonId) {
    this.rayonId = rayonId;
    return this;
  }

  public ProduitDTO setRayonId(long rayonId) {
    this.rayonId = rayonId;
    return this;
  }

  public ProduitDTO setTauxRemise(float tauxRemise) {
    this.tauxRemise = tauxRemise;
    return this;
  }

  public ProduitDTO setStatus(int status) {
    this.status = status;
    return this;
  }

  public ProduitDTO dailyStocks(List<DailyStock> dailyStocks) {
    this.dailyStocks = dailyStocks;
    return this;
  }
}
