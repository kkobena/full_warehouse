package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class ProduitDTO {
    private Long id;
    private String libelle;
    private int itemQuantity, qtyUG;
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
    private List<ProduitDTO> produits = new ArrayList<>();
    private Instant lastDateOfSale;
    private Instant lastOrderDate;
    private Instant lastInventoryDate;
    private Integer prixMnp = 0;
    public String codeCip;
    private Long parentId,fournisseurId;
    private String parentLibelle;
    private Long laboratoireId;
    private String laboratoireLibelle;
    private Long formeId;
    private String formeLibelle;
    private Long typeEtyquetteId;
    private String typeEtyquetteLibelle;
    private Long familleId;
    private String familleLibelle;
    private Long gammeId;
    private String gammeLibelle;
    private Long tvaId;
    private Integer tvaTaux;
    private Set<StockProduitDTO> stockProduits = new HashSet<>();
    private Set<FournisseurProduitDTO> fournisseurProduits = new HashSet<>();
    private StockProduitDTO stockProduit;
    private FournisseurProduitDTO fournisseurProduit;
    private String qtyStatus;
    private Integer qtyAppro = 0;
    private Integer qtySeuilMini = 0;
    private Boolean dateperemption = false;
    private Boolean chiffre = true;
    private int totalQuantity, qtyReserve;
    private Boolean deconditionnable  = false;
    private String codeEan, rayonLibelle;
    private Long remiseId, rayonId;
    private float tauxRemise;
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate perimeAt;
    private int status;
    private int saleOfPointStock, saleOfPointVirtualStock;
    private String expirationDate;

    public int getSaleOfPointStock() {
        return saleOfPointStock;
    }

    public int getQtyUG() {
        return qtyUG;
    }

    public ProduitDTO setQtyUG(int qtyUG) {
        this.qtyUG = qtyUG;
        return this;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public ProduitDTO setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
        return this;
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

    public ProduitDTO setPerimeAt(LocalDate perimeAt) {
        this.perimeAt = perimeAt;
        return this;
    }

    public int getQtyReserve() {
        return qtyReserve;
    }

    public ProduitDTO setQtyReserve(int qtyReserve) {
        this.qtyReserve = qtyReserve;
        return this;
    }

    public ProduitDTO() {

    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public ProduitDTO setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
        return this;
    }

    public ProduitDTO(Produit produit) {
        this.id = produit.getId();
        this.libelle = produit.getLibelle();
        this.itemQuantity = produit.getProduits().stream().collect(Collectors.summingInt(Produit::getItemQty));
        this.typeProduit = produit.getTypeProduit();
        this.costAmount = produit.getCostAmount();
        this.regularUnitPrice = produit.getRegularUnitPrice();
        this.netUnitPrice = produit.getNetUnitPrice();
        this.createdAt = produit.getCreatedAt();
        this.updatedAt = produit.getUpdatedAt();
        this.itemQty = produit.getItemQty();
        this.itemCostAmount = produit.getItemCostAmount();
        this.itemRegularUnitPrice = produit.getItemRegularUnitPrice();
        Produit parent = produit.getParent();
        if (parent != null) {
            this.produitId = parent.getId();
            this.produitLibelle = parent.getLibelle();
        }
        this.produits = produit.getProduits().stream().map(ProduitDTO::new).collect(Collectors.toList());
        this.fournisseurProduits = produit.getFournisseurProduits().stream().map(FournisseurProduitDTO::new).collect(Collectors.toSet());
        this.chiffre = produit.getChiffre();
        this.createdAt = produit.getCreatedAt();
        // System.out.println(produit.getFournisseurProduitPrincipal());
        this.prixMnp = produit.getPrixMnp();
        FournisseurProduit fournisseurProduitPrincipal = produit.getFournisseurProduitPrincipal();
        if (fournisseurProduitPrincipal != null) {
            this.codeCip = fournisseurProduitPrincipal.getCodeCip();
            this.fournisseurProduit = new FournisseurProduitDTO(fournisseurProduitPrincipal);
            this.fournisseurId = this.fournisseurProduit.getFournisseurId();
        }
        Laboratoire laboratoire = produit.getLaboratoire();
        if (laboratoire != null) {
            this.laboratoireId = laboratoire.getId();
            this.laboratoireLibelle = laboratoire.getLibelle();
        }
        FormProduit formProduit = produit.getForme();
        if (formProduit != null) {
            this.formeId = formProduit.getId();
            this.formeLibelle = formProduit.getLibelle();
        }
        TypeEtiquette typeEtiquette = produit.getTypeEtyquette();
        if (typeEtiquette != null) {
            this.typeEtyquetteId = typeEtiquette.getId();
            this.typeEtyquetteLibelle = typeEtiquette.getLibelle();
        }
        FamilleProduit familleProduit = produit.getFamille();
        if (familleProduit != null) {
            this.familleId = familleProduit.getId();
            this.familleLibelle = familleProduit.getLibelle();
        }
        GammeProduit gammeProduit = produit.getGamme();
        if (gammeProduit != null) {
            this.gammeId = gammeProduit.getId();
            this.gammeLibelle = gammeProduit.getLibelle();
        }
        Tva tva = produit.getTva();
        if (tva != null) {
            this.tvaId = tva.getId();
            this.tvaTaux = tva.getTaux();
        }
        this.stockProduits = produit.getStockProduits().stream().map(StockProduitDTO::new).collect(Collectors.toSet());
        StockProduit stockProduitPointOfSale = produit.getStockProduitPointOfSale();
        if (stockProduitPointOfSale != null) {
            this.stockProduit = new StockProduitDTO(stockProduitPointOfSale);
            this.saleOfPointStock = stockProduitPointOfSale.getQtyStock();
            Rayon rayon = stockProduitPointOfSale.getRayon();
            this.rayonId = rayon.getId();
            this.rayonLibelle = rayon.getLibelle();
        }
        this.totalQuantity = this.stockProduits.stream().collect(Collectors.summingInt(StockProduitDTO::getQtyStock));
        this.qtyAppro = produit.getQtyAppro();
        this.qtySeuilMini = produit.getQtySeuilMini();
        this.dateperemption = produit.getDateperemption();
        this.chiffre = produit.getChiffre();
        this.deconditionnable = produit.getDeconditionnable();
        this.codeEan = produit.getCodeEan();
        RemiseProduit remiseProduit = produit.getRemise();
        if (remiseProduit != null) {
            this.remiseId = remiseProduit.getId();
            this.tauxRemise = remiseProduit.getRemiseValue();
        }
        this.perimeAt = produit.getPerimeAt();
        if(produit.getPerimeAt()!=null){
            this.expirationDate=produit.getPerimeAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        this.status = produit.getStatus().ordinal();

    }
    public static FournisseurProduit fournisseurProduitFromDTO(ProduitDTO dto) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setCreatedAt(Instant.now());
        fournisseurProduit.setUpdatedAt(fournisseurProduit.getCreatedAt());
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        fournisseurProduit.setCodeCip(dto.getCodeCip());
        fournisseurProduit.setPrincipal(true);
        fournisseurProduit.setPrixAchat(dto.getCostAmount());
        fournisseurProduit.setPrixUni(dto.getRegularUnitPrice());
        return fournisseurProduit;
    }


    public static StockProduit stockProduitFromProduitDTO(ProduitDTO dto) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setQtyStock(0);
        stockProduit.setQtyVirtual(0);
        stockProduit.setCreatedAt(Instant.now());
        stockProduit.setUpdatedAt(stockProduit.getCreatedAt());
        stockProduit.setQtyUG(0);
        stockProduit.setRayon(rayonFromId(dto.getRayonId()));
        return stockProduit;
    }
    public static StockProduit stockProduitFromDTO(StockProduitDTO dto, Rayon rayon) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setQtyStock(dto.getQtyStock());
        stockProduit.setQtyVirtual(dto.getQtyVirtual());
        stockProduit.setCreatedAt(Instant.now());
        stockProduit.setUpdatedAt(stockProduit.getCreatedAt());
        stockProduit.setQtyUG(dto.getQtyUG());
        stockProduit.setRayon(rayon);
        return stockProduit;
    }

    public static Produit fromDTO(ProduitDTO produitDTO) {
        Produit produit = new Produit();
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setTypeProduit(TypeProduit.PACKAGE);
        produit.setCreatedAt(Instant.now());
        produit.setUpdatedAt(produit.getCreatedAt());
        produit.setCostAmount(produitDTO.getCostAmount());
        if ( produitDTO.getDeconditionnable() ) {
            produit.setItemCostAmount(produitDTO.getItemCostAmount());
            produit.setItemQty(produitDTO.getItemQty());
            produit.setItemRegularUnitPrice(produitDTO.getItemRegularUnitPrice());
        }else{
            produit.setItemCostAmount(produitDTO.getCostAmount());
            produit.setItemQty(1);
            produit.setItemRegularUnitPrice(produitDTO.getRegularUnitPrice());
        }
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCodeEan(produitDTO.getCodeEan());
        produit.setDateperemption(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());
        if(StringUtils.isNotEmpty(produitDTO.getExpirationDate())){
            produit.setPerimeAt(LocalDate.parse(produitDTO.getExpirationDate(),DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        produit.setRemise(resmiseProduitFromId(produitDTO.getRemiseId()));
        produit.setTva(tvaFromId(produitDTO.getTvaId()));
        produit.setLaboratoire(laboratoireFromId(produitDTO.getLaboratoireId()));
        produit.setFamille(familleProduitFromId(produitDTO.getFamilleId()));
        produit.setGamme(gammeFromId(produitDTO.getRemiseId()));
        produit.setTypeEtyquette(typeEtiquetteFromId(produitDTO.getTypeEtyquetteId()));
        produit.setForme(formProduitFromId(produitDTO.getFormeId()));
        produit.addStockProduit(stockProduitFromProduitDTO(produitDTO));
        produit.addFournisseurProduit(fournisseurProduitFromDTO(produitDTO));
        return produit;
    }


    public static Produit fromDTO(ProduitDTO produitDTO, StockProduit stockProduit, List<FournisseurProduit> fournisseurProduit) {
        Produit produit = new Produit();
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(0);
        if (produitDTO.getTypeProduit() == TypeProduit.DETAIL) {
            produit.setParent(fromId(produitDTO.getProduitId()));
        }
        produit.setTypeProduit(produitDTO.getTypeProduit());
        produit.setCreatedAt(Instant.now());
        produit.setUpdatedAt(produit.getCreatedAt());
        produit.setCostAmount(produitDTO.getCostAmount());
        produit.setItemCostAmount(produitDTO.getItemCostAmount());
        produit.setItemQty(produitDTO.getItemQty());
        produit.setItemRegularUnitPrice(produitDTO.getItemRegularUnitPrice());
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCodeEan(produitDTO.getCodeEan());
        produit.setDateperemption(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());
        produit.setPerimeAt(produitDTO.getPerimeAt());
        produit.setRemise(resmiseProduitFromId(produitDTO.getRemiseId()));
        produit.setTva(tvaFromId(produitDTO.getTvaId()));
        produit.setLaboratoire(laboratoireFromId(produitDTO.getLaboratoireId()));
        produit.setFamille(familleProduitFromId(produitDTO.getFamilleId()));
        produit.setGamme(gammeFromId(produitDTO.getRemiseId()));
        produit.setTypeEtyquette(typeEtiquetteFromId(produitDTO.getTypeEtyquetteId()));
        produit.setForme(formProduitFromId(produitDTO.getFormeId()));
        produit.addStockProduit(stockProduit);
        fournisseurProduit.forEach(e -> {
            produit.addFournisseurProduit(e);
        });

        return produit;
    }

    public static RemiseProduit resmiseProduitFromId(Long id) {
        if (id == null) {
            return null;
        }
        RemiseProduit remiseProduit = new RemiseProduit();
        remiseProduit.setId(id);
        return remiseProduit;
    }

    public static Produit fromId(Long produitId) {
        if (produitId == null) {
            return null;
        }
        Produit produit = new Produit();
        produit.setId(produitId);
        return produit;
    }

    public static Tva tvaFromId(Long tvaId) {
        if (tvaId == null) {
            return null;
        }
        Tva tva = new Tva();
        tva.setId(tvaId);
        return tva;
    }

    public static FamilleProduit familleProduitFromId(Long id) {
        if (id == null) {
            return null;
        }
        FamilleProduit entity = new FamilleProduit();
        entity.setId(id);
        return entity;
    }

    public static GammeProduit gammeFromId(Long id) {
        if (id == null) {
            return null;
        }
        GammeProduit entity = new GammeProduit();
        entity.setId(id);
        return entity;
    }

    public static Laboratoire laboratoireFromId(Long id) {
        if (id == null) {
            return null;
        }
        Laboratoire entity = new Laboratoire();
        entity.setId(id);
        return entity;
    }

    public static TypeEtiquette typeEtiquetteFromId(Long id) {
        if (id == null) {
            return null;
        }
        TypeEtiquette entity = new TypeEtiquette();
        entity.setId(id);
        return entity;
    }


    public static FormProduit formProduitFromId(Long id) {
        if (id == null) {
            return null;
        }
        FormProduit entity = new FormProduit();
        entity.setId(id);
        return entity;
    }

    public static Fournisseur fournisseurFromId(Long id) {
        if (id == null) {
            return null;
        }
        Fournisseur entity = new Fournisseur();
        entity.setId(id);
        return entity;
    }

    public static Rayon rayonFromId(Long id) {
        if (id == null) {
            return null;
        }
        Rayon entity = new Rayon();
        entity.setId(id);
        return entity;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ProduitDTO setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public ProduitDTO setUpdatedAt(Instant updatedAt) {
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

    public Instant getLastDateOfSale() {
        return lastDateOfSale;
    }

    public ProduitDTO setLastDateOfSale(Instant lastDateOfSale) {
        this.lastDateOfSale = lastDateOfSale;
        return this;
    }

    public Instant getLastOrderDate() {
        return lastOrderDate;
    }

    public ProduitDTO setLastOrderDate(Instant lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
        return this;
    }

    public Instant getLastInventoryDate() {
        return lastInventoryDate;
    }

    public ProduitDTO setLastInventoryDate(Instant lastInventoryDate) {
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

    public String getCodeCip() {
        return codeCip;
    }

    public ProduitDTO setCodeCip(String codeCip) {
        this.codeCip = codeCip;
        return this;
    }

    public Long getParentId() {
        return parentId;
    }

    public ProduitDTO setParentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getParentLibelle() {
        return parentLibelle;
    }

    public ProduitDTO setParentLibelle(String parentLibelle) {
        this.parentLibelle = parentLibelle;
        return this;
    }

    public Long getLaboratoireId() {
        return laboratoireId;
    }

    public ProduitDTO setLaboratoireId(Long laboratoireId) {
        this.laboratoireId = laboratoireId;
        return this;
    }

    public String getLaboratoireLibelle() {
        return laboratoireLibelle;
    }

    public ProduitDTO setLaboratoireLibelle(String laboratoireLibelle) {
        this.laboratoireLibelle = laboratoireLibelle;
        return this;
    }

    public Long getFormeId() {
        return formeId;
    }

    public ProduitDTO setFormeId(Long formeId) {
        this.formeId = formeId;
        return this;
    }

    public String getFormeLibelle() {
        return formeLibelle;
    }

    public ProduitDTO setFormeLibelle(String formeLibelle) {
        this.formeLibelle = formeLibelle;
        return this;
    }

    public Long getTypeEtyquetteId() {
        return typeEtyquetteId;
    }

    public ProduitDTO setTypeEtyquetteId(Long typeEtyquetteId) {
        this.typeEtyquetteId = typeEtyquetteId;
        return this;
    }

    public String getTypeEtyquetteLibelle() {
        return typeEtyquetteLibelle;
    }

    public ProduitDTO setTypeEtyquetteLibelle(String typeEtyquetteLibelle) {
        this.typeEtyquetteLibelle = typeEtyquetteLibelle;
        return this;
    }

    public Long getFamilleId() {
        return familleId;
    }

    public ProduitDTO setFamilleId(Long familleId) {
        this.familleId = familleId;
        return this;
    }

    public String getFamilleLibelle() {
        return familleLibelle;
    }

    public ProduitDTO setFamilleLibelle(String familleLibelle) {
        this.familleLibelle = familleLibelle;
        return this;
    }

    public Long getGammeId() {
        return gammeId;
    }

    public ProduitDTO setGammeId(Long gammeId) {
        this.gammeId = gammeId;
        return this;
    }

    public String getGammeLibelle() {
        return gammeLibelle;
    }

    public ProduitDTO setGammeLibelle(String gammeLibelle) {
        this.gammeLibelle = gammeLibelle;
        return this;
    }

    public Long getTvaId() {
        return tvaId;
    }

    public ProduitDTO setTvaId(Long tvaId) {
        this.tvaId = tvaId;
        return this;
    }

    public Integer getTvaTaux() {
        return tvaTaux;
    }

    public ProduitDTO setTvaTaux(Integer tvaTaux) {
        this.tvaTaux = tvaTaux;
        return this;
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
        return this;
    }

    public String getQtyStatus() {
        return qtyStatus;
    }

    public ProduitDTO setQtyStatus(String qtyStatus) {
        this.qtyStatus = qtyStatus;
        return this;
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

    public Long getRemiseId() {
        return remiseId;
    }

    public ProduitDTO setRemiseId(Long remiseId) {
        this.remiseId = remiseId;
        return this;
    }

    public Long getRayonId() {
        return rayonId;
    }

    public ProduitDTO setRayonId(Long rayonId) {
        this.rayonId = rayonId;
        return this;
    }

    public float getTauxRemise() {
        return tauxRemise;
    }

    public ProduitDTO setTauxRemise(float tauxRemise) {
        this.tauxRemise = tauxRemise;
        return this;
    }

    public LocalDate getPerimeAt() {
        return perimeAt;
    }

    public int getStatus() {
        return status;
    }

    public ProduitDTO setStatus(int status) {
        this.status = status;
        return this;
    }


    public static ProduitDTO lite(Produit produit) {
        ProduitDTO dto = new ProduitDTO();
        dto.setId(produit.getId());
        dto.setLibelle(produit.getLibelle());
        dto.setTypeProduit(produit.getTypeProduit());
        dto.setRegularUnitPrice(produit.getRegularUnitPrice());
        dto.setNetUnitPrice(produit.getNetUnitPrice());
        StockProduit stockProduitPointOfSale = produit.getStockProduitPointOfSale();
        if (stockProduitPointOfSale != null) {
            //  dto.setStockProduit( new StockProduitDTO(stockProduitPointOfSale));
            dto.setSaleOfPointStock(stockProduitPointOfSale.getQtyStock());
            dto.setSaleOfPointVirtualStock(stockProduitPointOfSale.getQtyVirtual());
            Rayon rayon = stockProduitPointOfSale.getRayon();
            dto.setRayonId(rayon.getId());
            dto.setRayonLibelle(rayon.getLibelle());
        }
        dto.setCodeEan(produit.getCodeEan());
        RemiseProduit remiseProduit = produit.getRemise();
        if (remiseProduit != null) {
            dto.setRemiseId(remiseProduit.getId());
            dto.setTauxRemise(remiseProduit.getRemiseValue());
        }
        FournisseurProduit fournisseurProduitPrincipal = produit.getFournisseurProduitPrincipal();
        if (fournisseurProduitPrincipal != null) {
            dto.setCodeCip(fournisseurProduitPrincipal.getCodeCip());

        }
        return dto;
    }
}
