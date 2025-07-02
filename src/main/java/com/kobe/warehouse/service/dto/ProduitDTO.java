package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.kobe.warehouse.domain.DailyStock;
import com.kobe.warehouse.domain.HistoriqueProduitInventaire;
import com.kobe.warehouse.domain.ParcoursProduit;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.produit_prix.dto.PrixReferenceDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ProduitDTO {

    private Integer prixMnp = 0;
    private Set<StockProduitDTO> stockProduits;
    private Set<FournisseurProduitDTO> fournisseurProduits;
    private Boolean dateperemption = false;
    private Boolean chiffre = true;
    private List<RayonProduitDTO> rayonProduits;
    private List<ParcoursProduit> parcoursProduits;
    private String codeCip = "";
    private Long id;
    private String libelle;
    private int itemQuantity;
    private int qtyUG;
    private TypeProduit typeProduit;
    private Integer quantity;
    private Integer costAmount;
    private Integer regularUnitPrice;
    private Integer netUnitPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer itemQty;
    private Integer itemCostAmount;
    private Integer itemRegularUnitPrice;
    private Long produitId;
    private String produitLibelle;
    private int quantityReceived;
    private List<ProduitDTO> produits = new ArrayList<>();
    private LocalDateTime lastDateOfSale;
    private LocalDateTime lastOrderDate;
    private LocalDateTime lastInventoryDate;
    private Long parentId;
    private Long fournisseurId;
    private String parentLibelle;
    private Long laboratoireId;
    private String laboratoireLibelle;
    private Long formeId;
    private String formeLibelle;
    private Long familleId;
    private String familleLibelle;
    private Long gammeId;
    private String gammeLibelle;
    private Long tvaId;
    private Integer tvaTaux;
    private StockProduitDTO stockProduit;
    private FournisseurProduitDTO fournisseurProduit;
    private String qtyStatus;
    private Integer qtyAppro = 0;
    private Integer qtySeuilMini = 0;
    private int totalQuantity;
    private int qtyReserve;
    private Boolean deconditionnable = false;
    private String codeEan;
    private String rayonLibelle;
    private long rayonId;
    private long storageId;
    private List<PrixReferenceDTO> prixReference = new ArrayList<>();

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate perimeAt;

    private int status;
    private String displayStatut;
    private int saleOfPointStock;
    private int saleOfPointVirtualStock;
    private String expirationDate;
    private String displayField;
    private List<DailyStock> dailyStocks = new ArrayList<>();
    private TableauDTO tableau;
    private int unitPrice;
    private String remiseCode;
    private EtatProduit etatProduit;
    private Long dciId;
    private String dciLibelle;
    private String dciCode;
    private String categorie;
    private List<HistoriqueProduitInventaire> historiqueProduitInventaires= new ArrayList<>();

    public String getRemiseCode() {
        return remiseCode;
    }

    public ProduitDTO setRemiseCode(String remiseCode) {
        this.remiseCode = remiseCode;
        return this;
    }

    public List<PrixReferenceDTO> getPrixReference() {
        return prixReference;
    }

    public ProduitDTO setPrixReference(List<PrixReferenceDTO> prixReference) {
        this.prixReference = prixReference;
        return this;
    }

    public ProduitDTO displayStatut(String displayStatut) {
        this.displayStatut = displayStatut;
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

    public ProduitDTO familleId(Long familleId) {
        this.familleId = familleId;
        return this;
    }

    public ProduitDTO gammeId(Long gammeId) {
        this.gammeId = gammeId;
        return this;
    }

    public Long getDciId() {
        return dciId;
    }

    public ProduitDTO setDciId(Long dciId) {
        this.dciId = dciId;
        return this;
    }

    public String getDciLibelle() {
        return dciLibelle;
    }

    public ProduitDTO setDciLibelle(String dciLibelle) {
        this.dciLibelle = dciLibelle;
        return this;
    }

    public String getDciCode() {
        return dciCode;
    }

    public ProduitDTO setDciCode(String dciCode) {
        this.dciCode = dciCode;
        return this;
    }

    public String getCategorie() {
        return categorie;
    }

    public ProduitDTO setCategorie(String categorie) {
        this.categorie = categorie;
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

    public ProduitDTO dailyStocks(List<DailyStock> dailyStocks) {
        this.dailyStocks = dailyStocks;
        return this;
    }

    public String getCodeCip() {
        return codeCip;
    }

    public ProduitDTO setCodeCip(String codeCip) {
        this.codeCip = codeCip;
        return this;
    }

    public Long getId() {
        return id;
    }

    public ProduitDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public ProduitDTO setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public int getItemQuantity() {
        return itemQuantity;
    }

    public ProduitDTO setItemQuantity(int itemQuantity) {
        this.itemQuantity = itemQuantity;
        return this;
    }

    public int getQtyUG() {
        return qtyUG;
    }

    public ProduitDTO setQtyUG(int qtyUG) {
        this.qtyUG = qtyUG;
        return this;
    }

    public TypeProduit getTypeProduit() {
        return typeProduit;
    }

    public ProduitDTO setTypeProduit(TypeProduit typeProduit) {
        this.typeProduit = typeProduit;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ProduitDTO setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public EtatProduit getEtatProduit() {
        return etatProduit;
    }

    public ProduitDTO setEtatProduit(EtatProduit etatProduit) {
        this.etatProduit = etatProduit;
        return this;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public ProduitDTO setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public ProduitDTO setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
        return this;
    }

    public Integer getNetUnitPrice() {
        return netUnitPrice;
    }

    public ProduitDTO setNetUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ProduitDTO setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public ProduitDTO setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Integer getItemQty() {
        return itemQty;
    }

    public ProduitDTO setItemQty(Integer itemQty) {
        this.itemQty = itemQty;
        return this;
    }

    public Integer getItemCostAmount() {
        return itemCostAmount;
    }

    public ProduitDTO setItemCostAmount(Integer itemCostAmount) {
        this.itemCostAmount = itemCostAmount;
        return this;
    }

    public Integer getItemRegularUnitPrice() {
        return itemRegularUnitPrice;
    }

    public ProduitDTO setItemRegularUnitPrice(Integer itemRegularUnitPrice) {
        this.itemRegularUnitPrice = itemRegularUnitPrice;
        return this;
    }

    public Long getProduitId() {
        return produitId;
    }

    public ProduitDTO setProduitId(Long produitId) {
        this.produitId = produitId;
        return this;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public ProduitDTO setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public ProduitDTO setQuantityReceived(int quantityReceived) {
        this.quantityReceived = quantityReceived;
        return this;
    }

    public List<ProduitDTO> getProduits() {
        return produits;
    }

    public ProduitDTO setProduits(List<ProduitDTO> produits) {
        this.produits = produits;
        return this;
    }

    public LocalDateTime getLastDateOfSale() {
        return lastDateOfSale;
    }

    public ProduitDTO setLastDateOfSale(LocalDateTime lastDateOfSale) {
        this.lastDateOfSale = lastDateOfSale;
        return this;
    }

    public LocalDateTime getLastOrderDate() {
        return lastOrderDate;
    }

    public ProduitDTO setLastOrderDate(LocalDateTime lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
        return this;
    }

    public LocalDateTime getLastInventoryDate() {
        return lastInventoryDate;
    }

    public ProduitDTO setLastInventoryDate(LocalDateTime lastInventoryDate) {
        this.lastInventoryDate = lastInventoryDate;
        return this;
    }

    public Integer getPrixMnp() {
        return prixMnp;
    }

    public ProduitDTO setPrixMnp(Integer prixMnp) {
        this.prixMnp = prixMnp;
        return this;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public ProduitDTO setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
        return this;
    }

    public String getParentLibelle() {
        return parentLibelle;
    }

    public void setParentLibelle(String parentLibelle) {
        this.parentLibelle = parentLibelle;
    }

    public void parentLibelle(String parentLibelle) {
        this.parentLibelle = parentLibelle;
    }

    public Long getLaboratoireId() {
        return laboratoireId;
    }

    public void setLaboratoireId(Long laboratoireId) {
        this.laboratoireId = laboratoireId;
    }

    public String getLaboratoireLibelle() {
        return laboratoireLibelle;
    }

    public void setLaboratoireLibelle(String laboratoireLibelle) {
        this.laboratoireLibelle = laboratoireLibelle;
    }

    public Long getFormeId() {
        return formeId;
    }

    public void setFormeId(Long formeId) {
        this.formeId = formeId;
    }

    public String getFormeLibelle() {
        return formeLibelle;
    }

    public void setFormeLibelle(String formeLibelle) {
        this.formeLibelle = formeLibelle;
    }



    public Long getFamilleId() {
        return familleId;
    }

    public void setFamilleId(Long familleId) {
        this.familleId = familleId;
    }

    public String getFamilleLibelle() {
        return familleLibelle;
    }

    public void setFamilleLibelle(String familleLibelle) {
        this.familleLibelle = familleLibelle;
    }

    public Long getGammeId() {
        return gammeId;
    }

    public void setGammeId(Long gammeId) {
        this.gammeId = gammeId;
    }

    public String getGammeLibelle() {
        return gammeLibelle;
    }

    public void setGammeLibelle(String gammeLibelle) {
        this.gammeLibelle = gammeLibelle;
    }

    public Long getTvaId() {
        return tvaId;
    }

    public void setTvaId(Long tvaId) {
        this.tvaId = tvaId;
    }

    public Integer getTvaTaux() {
        return tvaTaux;
    }

    public void setTvaTaux(Integer tvaTaux) {
        this.tvaTaux = tvaTaux;
    }

    public Set<StockProduitDTO> getStockProduits() {
        return stockProduits;
    }

    public ProduitDTO setStockProduits(Set<StockProduitDTO> stockProduits) {
        this.stockProduits = stockProduits;
        return this;
    }

    public Set<FournisseurProduitDTO> getFournisseurProduits() {
        return fournisseurProduits;
    }

    public ProduitDTO setFournisseurProduits(Set<FournisseurProduitDTO> fournisseurProduits) {
        this.fournisseurProduits = fournisseurProduits;
        return this;
    }

    public StockProduitDTO getStockProduit() {
        return stockProduit;
    }

    public ProduitDTO setStockProduit(StockProduitDTO stockProduit) {
        this.stockProduit = stockProduit;
        return this;
    }

    public FournisseurProduitDTO getFournisseurProduit() {
        return fournisseurProduit;
    }

    public ProduitDTO setFournisseurProduit(FournisseurProduitDTO fournisseurProduit) {
        this.fournisseurProduit = fournisseurProduit;
        if (Objects.nonNull(fournisseurProduit)) {
            this.codeCip = fournisseurProduit.getCodeCip();
        }
        return this;
    }

    public String getQtyStatus() {
        return qtyStatus;
    }

    public void setQtyStatus(String qtyStatus) {
        this.qtyStatus = qtyStatus;
    }

    public Integer getQtyAppro() {
        return qtyAppro;
    }

    public ProduitDTO setQtyAppro(Integer qtyAppro) {
        this.qtyAppro = qtyAppro;
        return this;
    }

    public Integer getQtySeuilMini() {
        return qtySeuilMini;
    }

    public ProduitDTO setQtySeuilMini(Integer qtySeuilMini) {
        this.qtySeuilMini = qtySeuilMini;
        return this;
    }

    public Boolean getDateperemption() {
        return dateperemption;
    }

    public ProduitDTO setDateperemption(Boolean dateperemption) {
        this.dateperemption = dateperemption;
        return this;
    }

    public Boolean getChiffre() {
        return chiffre;
    }

    public ProduitDTO setChiffre(Boolean chiffre) {
        this.chiffre = chiffre;
        return this;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public ProduitDTO setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
        return this;
    }

    public int getQtyReserve() {
        return qtyReserve;
    }

    public void setQtyReserve(int qtyReserve) {
        this.qtyReserve = qtyReserve;
    }

    public Boolean getDeconditionnable() {
        return deconditionnable;
    }

    public ProduitDTO setDeconditionnable(Boolean deconditionnable) {
        this.deconditionnable = deconditionnable;
        return this;
    }

    public String getCodeEan() {
        return codeEan;
    }

    public ProduitDTO setCodeEan(String codeEan) {
        this.codeEan = codeEan;
        return this;
    }

    public String getRayonLibelle() {
        return rayonLibelle;
    }

    public ProduitDTO setRayonLibelle(String rayonLibelle) {
        this.rayonLibelle = rayonLibelle;
        return this;
    }

    public long getRayonId() {
        return rayonId;
    }

    public ProduitDTO setRayonId(Long rayonId) {
        this.rayonId = rayonId;
        return this;
    }

    public ProduitDTO setRayonId(long rayonId) {
        this.rayonId = rayonId;
        return this;
    }

    public long getStorageId() {
        return storageId;
    }

    public ProduitDTO setStorageId(Long storageId) {
        this.storageId = storageId;
        return this;
    }

    public ProduitDTO setStorageId(long storageId) {
        this.storageId = storageId;
        return this;
    }

    public LocalDate getPerimeAt() {
        return perimeAt;
    }

    public void setPerimeAt(LocalDate perimeAt) {
        this.perimeAt = perimeAt;
    }

    public int getStatus() {
        return status;
    }

    public ProduitDTO setStatus(int status) {
        this.status = status;
        return this;
    }

    public String getDisplayStatut() {
        return displayStatut;
    }

    public void setDisplayStatut(String displayStatut) {
        this.displayStatut = displayStatut;
    }

    public int getSaleOfPointStock() {
        return saleOfPointStock;
    }

    public ProduitDTO setSaleOfPointStock(int saleOfPointStock) {
        this.saleOfPointStock = saleOfPointStock;
        return this;
    }

    public int getSaleOfPointVirtualStock() {
        return saleOfPointVirtualStock;
    }

    public ProduitDTO setSaleOfPointVirtualStock(int saleOfPointVirtualStock) {
        this.saleOfPointVirtualStock = saleOfPointVirtualStock;
        return this;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getDisplayField() {
        return displayField;
    }

    public ProduitDTO setDisplayField(String displayField) {
        this.displayField = displayField;
        return this;
    }

    public List<RayonProduitDTO> getRayonProduits() {
        return rayonProduits;
    }

    public ProduitDTO setRayonProduits(List<RayonProduitDTO> rayonProduits) {
        this.rayonProduits = rayonProduits;
        return this;
    }

    public List<ParcoursProduit> getParcoursProduits() {
        return parcoursProduits;
    }

    public ProduitDTO setParcoursProduits(List<ParcoursProduit> parcoursProduits) {
        this.parcoursProduits = parcoursProduits;
        return this;
    }

    public List<DailyStock> getDailyStocks() {
        return dailyStocks;
    }

    public void setDailyStocks(List<DailyStock> dailyStocks) {
        this.dailyStocks = dailyStocks;
    }

    public TableauDTO getTableau() {
        return tableau;
    }

    public ProduitDTO setTableau(TableauDTO tableau) {
        this.tableau = tableau;
        return this;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public ProduitDTO setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public List<HistoriqueProduitInventaire> getHistoriqueProduitInventaires() {
        return historiqueProduitInventaires;
    }

    public ProduitDTO setHistoriqueProduitInventaires(List<HistoriqueProduitInventaire> historiqueProduitInventaires) {
        this.historiqueProduitInventaires = historiqueProduitInventaires;
        return this;
    }
}
