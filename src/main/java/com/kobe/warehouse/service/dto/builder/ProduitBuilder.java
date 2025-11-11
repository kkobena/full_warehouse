package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.FormProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.GammeProduit;
import com.kobe.warehouse.domain.Laboratoire;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.CategorieABC;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.RayonProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.dto.TableauDTO;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public final class ProduitBuilder {

    private static final String PERIME_DATE_PATERN = "dd/MM/yyyy";

    private ProduitBuilder() {}

    public static Produit fromDTO(ProduitDTO produitDTO, Rayon rayon) {
        Produit produit = new Produit();
        produit.setRayonProduits(Set.of(new RayonProduit().setProduit(produit).setRayon(rayon)));
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setCodeEanLaboratoire(produitDTO.getCodeEanLaboratoire());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setTypeProduit(TypeProduit.PACKAGE);
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(produit.getCreatedAt());
        if (org.springframework.util.StringUtils.hasText(produitDTO.getRemiseCode())) {
            produit.setCodeRemise(CodeRemise.fromValue(produitDTO.getRemiseCode()));
        }

        produit.setCostAmount(produitDTO.getCostAmount());
        if (produitDTO.getDeconditionnable()) {
            produit.setItemCostAmount(produitDTO.getItemCostAmount());
            produit.setItemQty(produitDTO.getItemQty());
            produit.setItemRegularUnitPrice(produitDTO.getItemRegularUnitPrice());
        } else {
            produit.setItemCostAmount(produitDTO.getCostAmount());
            produit.setItemQty(1);
            produit.setItemRegularUnitPrice(produitDTO.getRegularUnitPrice());
        }
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCodeEanLaboratoire(produitDTO.getLaboratoireLibelle());
        produit.setCheckExpiryDate(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());

        produit.setTva(tvaFromId(produitDTO.getTvaId()));
        produit.setLaboratoire(laboratoireFromId(produitDTO.getLaboratoireId()));
        produit.setFamille(familleProduitFromId(produitDTO.getFamilleId()));
        produit.setGamme(gammeFromId(produitDTO.getGammeId()));
        produit.setForme(formProduitFromId(produitDTO.getFormeId()));
        produit.addStockProduit(stockProduitFromProduitDTO(rayon.getStorage()));
        produit.addFournisseurProduit(fournisseurProduitFromDTO(produitDTO));
        if (org.springframework.util.StringUtils.hasLength(produitDTO.getCategorie())) {
            produit.setCategorie(CategorieABC.valueOf(produitDTO.getCategorie()));
        }
        produit.setDci(dciFromId(produitDTO.getDciId()));

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
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(produit.getCreatedAt());
        produit.setCostAmount(produitDTO.getCostAmount());
        produit.setItemCostAmount(produitDTO.getItemCostAmount());
        produit.setItemQty(produitDTO.getItemQty());
        produit.setItemRegularUnitPrice(produitDTO.getItemRegularUnitPrice());
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCodeEanLaboratoire(produitDTO.getLaboratoireLibelle());
        produit.setCheckExpiryDate(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());
        produit.setTva(tvaFromId(produitDTO.getTvaId()));
        produit.setLaboratoire(laboratoireFromId(produitDTO.getLaboratoireId()));
        produit.setFamille(familleProduitFromId(produitDTO.getFamilleId()));
        produit.setGamme(gammeFromId(produitDTO.getGammeId()));
        produit.setForme(formProduitFromId(produitDTO.getFormeId()));
        produit.addStockProduit(stockProduit);
        fournisseurProduit.forEach(produit::addFournisseurProduit);
        return produit;
    }

    public static Produit fromId(Integer produitId) {
        if (produitId == null) {
            return null;
        }
        Produit produit = new Produit();
        produit.setId(produitId);
        return produit;
    }

    private static ProduitDTO laboratoire(ProduitDTO produitDTO, Produit produit) {
        Laboratoire laboratoire = produit.getLaboratoire();
        if (laboratoire != null) {
            produitDTO.laboratoireId(laboratoire.getId()).laboratoireLibelle(laboratoire.getLibelle());
        }
        return produitDTO;
    }

    private static ProduitDTO formProduit(ProduitDTO produitDTO, Produit produit) {
        FormProduit formProduit = produit.getForme();
        if (formProduit != null) {
            produitDTO.formeId(formProduit.getId()).formeLibelle(formProduit.getLibelle());
        }
        return produitDTO;
    }

    private static void updateCategorieABC(ProduitDTO produitDTO, Produit produit) {
        if (produit.getCategorie() != null) {
            produitDTO.setCategorie(produit.getCategorie().name());
        }
    }

    private static void updateDci(ProduitDTO produitDTO, Produit produit) {
        Dci dci = produit.getDci();
        if (dci != null) {
            produitDTO.setDciId(dci.getId()).setDciLibelle(dci.getLibelle());
        }
    }

    private static ProduitDTO familleProduit(ProduitDTO produitDTO, Produit produit) {
        FamilleProduit familleProduit = produit.getFamille();
        if (familleProduit != null) {
            produitDTO.familleId(familleProduit.getId()).familleLibelle(familleProduit.getLibelle());
        }
        return produitDTO;
    }

    private static ProduitDTO gammeProduit(ProduitDTO produitDTO, Produit produit) {
        GammeProduit gammeProduit = produit.getGamme();
        if (gammeProduit != null) {
            produitDTO.gammeId(gammeProduit.getId()).gammeLibelle(gammeProduit.getLibelle());
        }
        return produitDTO;
    }

    public static ProduitDTO tva(ProduitDTO produitDTO, Produit produit) {
        Tva tva = produit.getTva();
        if (tva != null) {
            produitDTO.tvaId(tva.getId()).tvaTaux(tva.getTaux());
        }
        return produitDTO;
    }

    public static ProduitDTO stockProduits(ProduitDTO produitDTO, Produit produit, Integer magasinId) {
        produitDTO.setStockProduits(
            produit
                .getStockProduits()
                .stream()
                .filter(s -> s.getStorage().getMagasin().getId().equals(magasinId))
                .map(StockProduitDTO::new)
                .collect(Collectors.toSet())
        );
        produitDTO.setTotalQuantity(produitDTO.getStockProduits().stream().mapToInt(StockProduitDTO::getQtyStock).sum());
        return produitDTO;
    }

    public static ProduitDTO stockProduit(ProduitDTO produitDTO, StockProduit stockProduitPointOfSale) {
        if (stockProduitPointOfSale != null) {
            produitDTO
                .setStockProduit(new StockProduitDTO(stockProduitPointOfSale))
                .setSaleOfPointStock(stockProduitPointOfSale.getQtyStock());
        }
        return produitDTO;
    }

    public static ProduitDTO stockProduits(ProduitDTO produitDTO, Produit produit) {
        produitDTO.setStockProduits(produit.getStockProduits().stream().map(StockProduitDTO::new).collect(Collectors.toSet()));
        StockProduit stockProduitPointOfSale = produit.getStockProduitPointOfSale();
        if (stockProduitPointOfSale != null) {
            produitDTO.setStockProduit(new StockProduitDTO(stockProduitPointOfSale));
            produitDTO.setSaleOfPointStock(stockProduitPointOfSale.getQtyStock());
        }
        return produitDTO;
    }

    public static ProduitDTO rayonProduits(ProduitDTO produitDTO, Produit produit, Integer magasinId) {
        Set<RayonProduit> rayonProduits = produit.getRayonProduits();
        if (!CollectionUtils.isEmpty(rayonProduits)) {
            produitDTO.setRayonProduits(rayonProduits.stream().map(RayonProduitDTO::new).toList());

            Optional<RayonProduitDTO> rayon = produitDTO
                .getRayonProduits()
                .stream()
                .filter(r -> (r.getStorageType().equalsIgnoreCase(StorageType.PRINCIPAL.getValue()) && r.getMagasinId().equals(magasinId)))
                .findFirst();
            rayon.ifPresent(rayonProduitDTO ->
                produitDTO.setRayonId(rayonProduitDTO.getRayonId()).setRayonLibelle(rayonProduitDTO.getLibelleRayon())
            );
        }
        return produitDTO;
    }

    private static void setFournisseurPrincipal(ProduitDTO produitDTO, Produit produit) {
        FournisseurProduitDTO fournisseurProduit = fromPrincipal(produit);
        if (Objects.nonNull(fournisseurProduit)) {
            produitDTO
                .setCodeCip(fournisseurProduit.getCodeCip())
                .setFournisseurId(fournisseurProduit.getFournisseurId())
                .setCostAmount(fournisseurProduit.getPrixAchat())
                .setRegularUnitPrice(fournisseurProduit.getPrixUni());
        }
    }

    public static ProduitDTO buildFromProduit(Produit produit, Magasin magasin, StockProduit stockProduitPointOfSale) {
        ProduitDTO produitDTO = partialFromProduit(produit);
        setFournisseurPrincipal(produitDTO, produit);
        laboratoire(produitDTO, produit);
        formProduit(produitDTO, produit);
        familleProduit(produitDTO, produit);
        gammeProduit(produitDTO, produit);
        tva(produitDTO, produit);
        stockProduits(produitDTO, produit, magasin.getId());
        stockProduit(produitDTO, stockProduitPointOfSale);
        rayonProduits(produitDTO, produit, magasin.getId());
        produits(produitDTO, produit);
        fournisseurProduits(produitDTO, produit);
        produitDTO.setTableau(Optional.ofNullable(produit.getTableau()).map(TableauDTO::new).orElse(null));
        updateCategorieABC(produitDTO, produit);
        updateDci(produitDTO, produit);
        return produitDTO;
    }

    public static ProduitDTO produits(ProduitDTO produitDTO, Produit produit) {
        return produitDTO.setProduits(produit.getProduits().stream().map(ProduitBuilder::fromProduit).toList());
    }

    public static ProduitDTO fournisseurProduits(ProduitDTO produitDTO, Produit produit) {
        produitDTO.setFournisseurProduits(
            produit.getFournisseurProduits().stream().map(FournisseurProduitDTO::new).collect(Collectors.toSet())
        );
        return produitDTO;
    }

    public static ProduitDTO partialFromProduit(Produit produit) {
        Produit parent = produit.getParent();
        ProduitDTO produitDTO = new ProduitDTO()
            .setId(produit.getId())
            .setCodeEanLaboratoire(produit.getCodeEanLaboratoire())
            .setRemiseCode(produit.getCodeRemise().getValue())
            .setLibelle(produit.getLibelle())
            .setTypeProduit(produit.getTypeProduit())
            .setCostAmount(produit.getCostAmount())
            .setRegularUnitPrice(produit.getRegularUnitPrice())
            .setNetUnitPrice(produit.getNetUnitPrice())
            .setCreatedAt(produit.getCreatedAt())
            .setUpdatedAt(produit.getUpdatedAt())
            .setItemQty(produit.getItemQty())
            .setItemQuantity(produit.getProduits().stream().mapToInt(Produit::getItemQty).sum())
            .setItemCostAmount(produit.getItemCostAmount())
            .setItemRegularUnitPrice(produit.getItemRegularUnitPrice())
            .setProduitId(Objects.nonNull(parent) ? parent.getId() : null)
            .setTypeProduit(produit.getTypeProduit())
            .setProduitLibelle(Objects.nonNull(parent) ? parent.getLibelle() : null);
        produitDTO.setQtyAppro(produit.getQtyAppro()).setQtySeuilMini(produit.getQtySeuilMini());
        produitDTO.setDateperemption(produit.getCheckExpiryDate());
        produitDTO.setChiffre(produit.getChiffre());
        produitDTO.setDeconditionnable(produit.getDeconditionnable()).setCodeEan(produit.getCodeEanLaboratoire());

        produitDTO.setStatus(produit.getStatus().ordinal());
        produitDTO.displayStatut(produit.getStatus().name());
        if (!CollectionUtils.isEmpty(produitDTO.getHistoriqueProduitInventaires())) {
            produitDTO.setLastInventoryDate(produitDTO.getHistoriqueProduitInventaires().getFirst().dateInventaire());
        }

        return produitDTO;
    }

    public static ProduitDTO rayonProduits(ProduitDTO produitDTO, Produit produit) {
        Set<RayonProduit> rayonProduits = produit.getRayonProduits();
        if (!CollectionUtils.isEmpty(rayonProduits)) {
            produitDTO.setRayonProduits(rayonProduits.stream().map(RayonProduitDTO::new).toList());

            Optional<RayonProduit> rayon = produit
                .getRayonProduits()
                .stream()
                .filter(r -> r.getRayon().getStorage().getStorageType() == StorageType.PRINCIPAL)
                .findFirst();

            if (rayon.isPresent()) {
                Rayon rayonProduitDTO = rayon.get().getRayon();
                produitDTO.setRayonId(rayonProduitDTO.getId()).setRayonLibelle(rayonProduitDTO.getLibelle());
            }
        }
        return produitDTO;
    }

    public static ProduitDTO fromProduit(Produit produit) {
        ProduitDTO produitDTO = partialFromProduit(produit);
        setFournisseurPrincipal(produitDTO, produit);
        fournisseurProduits(produitDTO, produit);
        laboratoire(produitDTO, produit);
        formProduit(produitDTO, produit);
        familleProduit(produitDTO, produit);
        gammeProduit(produitDTO, produit);
        tva(produitDTO, produit);
        stockProduits(produitDTO, produit);
        rayonProduits(produitDTO, produit);
        produits(produitDTO, produit);
        produitDTO.setStatus(produit.getStatus().ordinal());
        produitDTO.setDisplayField(
            String.format("%s %s %d", produitDTO.getCodeCip(), produitDTO.getLibelle(), produitDTO.getRegularUnitPrice())
        );

        return produitDTO;
    }

    public static ProduitDTO lite(Produit produit) {
        ProduitDTO dto = new ProduitDTO();
        dto.setId(produit.getId());
        dto.setLibelle(produit.getLibelle());
        dto.setRemiseCode(produit.getCodeRemise().getValue());
        dto.setTypeProduit(produit.getTypeProduit());
        dto.setRegularUnitPrice(produit.getRegularUnitPrice());
        dto.setNetUnitPrice(produit.getNetUnitPrice());
        StockProduit stockProduitPointOfSale = produit.getStockProduitPointOfSale();
        if (stockProduitPointOfSale != null) {
            dto.setSaleOfPointStock(stockProduitPointOfSale.getQtyStock());
            dto.setSaleOfPointVirtualStock(stockProduitPointOfSale.getQtyVirtual());
            try {
                Rayon rayon = produit
                    .getRayonProduits()
                    .stream()
                    .filter(r -> r.getRayon().getStorage().getStorageType() == StorageType.PRINCIPAL)
                    .findFirst()
                    .get()
                    .getRayon();
                dto.setRayonId(rayon.getId());
                dto.setRayonLibelle(rayon.getLibelle());
            } catch (Exception _) {}
        }
        dto.setCodeEan(produit.getCodeEanLaboratoire());
        FournisseurProduit fournisseurProduitPrincipal = produit.getFournisseurProduitPrincipal();
        if (fournisseurProduitPrincipal != null) {
            dto.setCodeCip(fournisseurProduitPrincipal.getCodeCip());
        }

        return dto;
    }

    public static Dci dciFromId(Integer id) {
        if (id == null) {
            return null;
        }
        Dci entity = new Dci();
        entity.setId(id);
        return entity;
    }

    public static Tva tvaFromId(Integer tvaId) {
        if (tvaId == null) {
            return null;
        }
        Tva tva = new Tva();
        tva.setId(tvaId);
        return tva;
    }

    public static Laboratoire laboratoireFromId(Integer id) {
        if (id == null) {
            return null;
        }
        Laboratoire entity = new Laboratoire();
        entity.setId(id);
        return entity;
    }

    public static FormProduit formProduitFromId(Integer id) {
        if (id == null) {
            return null;
        }
        FormProduit entity = new FormProduit();
        entity.setId(id);
        return entity;
    }

    public static Fournisseur fournisseurFromId(Integer id) {
        if (id == null) {
            return null;
        }
        Fournisseur entity = new Fournisseur();
        entity.setId(id);
        return entity;
    }

    public static FamilleProduit familleProduitFromId(Integer id) {
        if (id == null) {
            return null;
        }
        FamilleProduit entity = new FamilleProduit();
        entity.setId(id);
        return entity;
    }

    public static GammeProduit gammeFromId(Integer id) {
        if (id == null) {
            return null;
        }
        GammeProduit entity = new GammeProduit();
        entity.setId(id);
        return entity;
    }

    public static StockProduit stockProduitFromProduitDTO(Storage storage) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setQtyStock(0);
        stockProduit.setQtyVirtual(0);
        stockProduit.setCreatedAt(LocalDateTime.now());
        stockProduit.setUpdatedAt(stockProduit.getCreatedAt());
        stockProduit.setQtyUG(0);
        stockProduit.setStorage(storage);
        return stockProduit;
    }

    public static FournisseurProduit fournisseurProduitFromDTO(ProduitDTO dto) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        fournisseurProduit.setCodeCip(dto.getCodeCip());
        fournisseurProduit.setPrixAchat(dto.getCostAmount());
        fournisseurProduit.setPrixUni(dto.getRegularUnitPrice());
        return fournisseurProduit;
    }

    public static FournisseurProduitDTO fromPrincipal(Produit produit) {
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        return Optional.ofNullable(fournisseurProduit).map(FournisseurProduitDTO::new).orElse(null);
    }

    public static Produit buildProduitFromProduitDTO(ProduitDTO produitDTO, Produit produit) {
        produit.setUpdatedAt(LocalDateTime.now());
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCostAmount(produitDTO.getCostAmount());
        if (produitDTO.getDeconditionnable()) {
            produit.setItemCostAmount(produitDTO.getItemCostAmount());
            produit.setItemQty(produitDTO.getItemQty());
            produit.setItemRegularUnitPrice(produitDTO.getItemRegularUnitPrice());
        } else {
            produit.setItemCostAmount(produitDTO.getCostAmount());
            produit.setItemQty(1);
            produit.setItemRegularUnitPrice(produitDTO.getRegularUnitPrice());
        }
        if (org.springframework.util.StringUtils.hasText(produitDTO.getRemiseCode())) {
            produit.setCodeRemise(CodeRemise.fromValue(produitDTO.getRemiseCode()));
        }
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCodeEanLaboratoire(produitDTO.getLaboratoireLibelle());
        produit.setCheckExpiryDate(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());

        produit.setTva(tvaFromId(produitDTO.getTvaId()));
        produit.setLaboratoire(laboratoireFromId(produitDTO.getLaboratoireId()));
        produit.setFamille(familleProduitFromId(produitDTO.getFamilleId()));
        produit.setGamme(gammeFromId(produitDTO.getGammeId()));
        produit.setForme(formProduitFromId(produitDTO.getFormeId()));
        produit.addFournisseurProduit(fournisseurProduitProduit(produit, produitDTO));
        return produit;
    }

    public static FournisseurProduit fournisseurProduitProduit(Produit produit, ProduitDTO dto) {
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        fournisseurProduit.setCodeCip(dto.getCodeCip());
        fournisseurProduit.setPrixAchat(dto.getCostAmount());
        fournisseurProduit.setPrixUni(dto.getRegularUnitPrice());
        fournisseurProduit.setProduit(produit);
        return fournisseurProduit;
    }

    public static ProduitDTO fromProduitWithRequiredParentRelation(Produit produit) {
        ProduitDTO produitDTO = partialFromProduit(produit);
        tva(produitDTO, produit);
        rayonProduits(produitDTO, produit);
        produitDTO.setStatus(produit.getStatus().ordinal());
        produitDTO.setTableau(Optional.ofNullable(produit.getTableau()).map(TableauDTO::new).orElse(null));
        produitDTO.setFournisseurProduit(
            Optional.ofNullable(produit.getFournisseurProduitPrincipal()).map(FournisseurProduitDTO::new).orElse(null)
        );
        produitDTO.setDisplayField(buildDisplayName(produitDTO));
        setUnitPrice(produitDTO);
        return produitDTO;
    }

    public static String buildDisplayName(ProduitDTO produitDTO) {
        FournisseurProduitDTO fournisseurProduitDTO = produitDTO.getFournisseurProduit();
        TableauDTO tableau = produitDTO.getTableau();
        if (Objects.nonNull(fournisseurProduitDTO)) {
            return String.format(
                "%s %s %s ",
                produitDTO.getLibelle(),
                fournisseurProduitDTO.getCodeCip(),
                NumberUtil.formatToString(
                    Optional.ofNullable(tableau)
                        .map(t -> t.getValue() + fournisseurProduitDTO.getPrixUni())
                        .orElse(fournisseurProduitDTO.getPrixUni())
                )
            );
        }
        return String.format(
            "%s %s %s ",
            produitDTO.getLibelle(),
            produitDTO.getCodeEan(),
            NumberUtil.formatToString(
                Optional.ofNullable(tableau)
                    .map(t -> t.getValue() + produitDTO.getRegularUnitPrice())
                    .orElse(produitDTO.getRegularUnitPrice())
            )
        );
    }

    public static void setUnitPrice(ProduitDTO produitDTO) {
        FournisseurProduitDTO fournisseurProduitDTO = produitDTO.getFournisseurProduit();

        if (Objects.nonNull(fournisseurProduitDTO)) {
            produitDTO.setUnitPrice(
                Optional.ofNullable(produitDTO.getTableau())
                    .map(t -> t.getValue() + fournisseurProduitDTO.getPrixUni())
                    .orElse(fournisseurProduitDTO.getPrixUni())
            );
        } else {
            produitDTO.setUnitPrice(
                Optional.ofNullable(produitDTO.getTableau())
                    .map(t -> t.getValue() + produitDTO.getRegularUnitPrice())
                    .orElse(produitDTO.getRegularUnitPrice())
            );
        }
    }

    public static ProduitDTO fromEntity(Produit produit) {
        ProduitDTO produitDTO = partialFromProduit(produit);
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        tva(produitDTO, produit);
        rayonProduits(produitDTO, produit);
        produitDTO.setStatus(produit.getStatus().ordinal());
        produitDTO.setTableau(Optional.ofNullable(produit.getTableau()).map(TableauDTO::new).orElse(null));
        produitDTO.setDisplayField(buildDisplayName(produitDTO, fournisseurProduit));
        setUnitPrice(produitDTO, fournisseurProduit);
        return produitDTO;
    }

    public static String buildDisplayName(ProduitDTO produitDTO, FournisseurProduit fournisseurProduit) {
        if (Objects.nonNull(fournisseurProduit)) {
            return String.format(
                "%s %s %s ",
                produitDTO.getLibelle(),
                fournisseurProduit.getCodeCip(),
                NumberUtil.formatToString(
                    Optional.ofNullable(produitDTO.getTableau())
                        .map(t -> t.getValue() + fournisseurProduit.getPrixUni())
                        .orElse(fournisseurProduit.getPrixUni())
                )
            );
        }
        return String.format(
            "%s %s %s ",
            produitDTO.getLibelle(),
            produitDTO.getCodeEan(),
            NumberUtil.formatToString(
                Optional.ofNullable(produitDTO.getTableau())
                    .map(t -> t.getValue() + produitDTO.getRegularUnitPrice())
                    .orElse(produitDTO.getRegularUnitPrice())
            )
        );
    }

    public static void setUnitPrice(ProduitDTO produitDTO, FournisseurProduit fournisseurProduit) {
        if (Objects.nonNull(fournisseurProduit)) {
            produitDTO.setUnitPrice(
                Optional.ofNullable(produitDTO.getTableau())
                    .map(t -> t.getValue() + fournisseurProduit.getPrixUni())
                    .orElse(fournisseurProduit.getPrixUni())
            );
        } else {
            produitDTO.setUnitPrice(
                Optional.ofNullable(produitDTO.getTableau())
                    .map(t -> t.getValue() + produitDTO.getRegularUnitPrice())
                    .orElse(produitDTO.getRegularUnitPrice())
            );
        }
    }

    public static ProduitDTO fromProductLiteList(Produit produit, StockProduit stockProduitPointOfSale, Magasin magasin) {
        ProduitDTO produitDTO = partialFromProduit(produit);
        rayonProduits(produitDTO, produit);
        produitDTO.setStatus(produit.getStatus().ordinal());
        stockProduits(produitDTO, produit, magasin.getId());
        stockProduit(produitDTO, stockProduitPointOfSale);
        produitDTO.setFournisseurProduit(
            Optional.ofNullable(produit.getFournisseurProduitPrincipal()).map(FournisseurProduitDTO::new).orElse(null)
        );
        //  produitDTO.setDisplayField(buildDisplayName(produitDTO));
        setUnitPrice(produitDTO);
        return produitDTO;
    }
}
