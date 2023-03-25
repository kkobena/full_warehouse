package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.domain.DailyStock;
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
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.TypeEtiquette;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.RayonProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

@Slf4j
public final class ProduitBuilder {

    private final static String PERIME_DATE_PATERN = "dd/MM/yyyy";

    public static Produit fromDTO(ProduitDTO produitDTO, Rayon rayon) {
        Produit produit = new Produit();
        produit.setRayonProduits(Set.of(new RayonProduit().setProduit(produit).setRayon(rayon)));
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setTypeProduit(TypeProduit.PACKAGE);
        produit.setCreatedAt(Instant.now());
        produit.setUpdatedAt(produit.getCreatedAt());
        produit.setCmuAmount(produitDTO.getCmuAmount());
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
        produit.setCodeEan(produitDTO.getCodeEan());
        produit.setCheckExpiryDate(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());
        if (StringUtils.isNotEmpty(produitDTO.getExpirationDate())) {
            produit.setPerimeAt(LocalDate.parse(produitDTO.getExpirationDate(),
                DateTimeFormatter.ofPattern(PERIME_DATE_PATERN)));
        }

        produit.setRemise(resmiseProduitFromId(produitDTO.getRemiseId()));
        produit.setTva(tvaFromId(produitDTO.getTvaId()));
        produit.setLaboratoire(laboratoireFromId(produitDTO.getLaboratoireId()));
        produit.setFamille(familleProduitFromId(produitDTO.getFamilleId()));
        produit.setGamme(gammeFromId(produitDTO.getRemiseId()));
        produit.setTypeEtyquette(typeEtiquetteFromId(produitDTO.getTypeEtiquetteId()));
        produit.setForme(formProduitFromId(produitDTO.getFormeId()));
        produit.addStockProduit(stockProduitFromProduitDTO(rayon.getStorage()));
        produit.addFournisseurProduit(fournisseurProduitFromDTO(produitDTO));

        return produit;
    }

    public static Produit fromDTO(ProduitDTO produitDTO, StockProduit stockProduit,
        List<FournisseurProduit> fournisseurProduit) {
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
        produit.setCheckExpiryDate(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());
        produit.setPerimeAt(produitDTO.getPerimeAt());
        produit.setRemise(resmiseProduitFromId(produitDTO.getRemiseId()));
        produit.setTva(tvaFromId(produitDTO.getTvaId()));
        produit.setLaboratoire(laboratoireFromId(produitDTO.getLaboratoireId()));
        produit.setFamille(familleProduitFromId(produitDTO.getFamilleId()));
        produit.setGamme(gammeFromId(produitDTO.getRemiseId()));
        produit.setTypeEtyquette(typeEtiquetteFromId(produitDTO.getTypeEtiquetteId()));
        produit.setForme(formProduitFromId(produitDTO.getFormeId()));
        produit.addStockProduit(stockProduit);
        produit.setCmuAmount(produitDTO.getCmuAmount());
        fournisseurProduit.forEach(e -> produit.addFournisseurProduit(e));
        return produit;
    }

    public static Produit fromId(Long produitId) {
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
            produitDTO.laboratoireId(laboratoire.getId())
                .laboratoireLibelle(laboratoire.getLibelle());
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

    private static ProduitDTO TypeEtiquette(ProduitDTO produitDTO, Produit produit) {
        TypeEtiquette typeEtiquette = produit.getTypeEtyquette();
        if (typeEtiquette != null) {
            produitDTO.typeEtiquetteId(typeEtiquette.getId())
                .typeEtiquetteLibelle(typeEtiquette.getLibelle());
        }
        return produitDTO;
    }

    private static ProduitDTO familleProduit(ProduitDTO produitDTO, Produit produit) {
        FamilleProduit familleProduit = produit.getFamille();
        if (familleProduit != null) {
            produitDTO.familleId(familleProduit.getId())
                .familleLibelle(familleProduit.getLibelle());

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

    public static ProduitDTO stockProduits(ProduitDTO produitDTO, Produit produit, Long magasinId) {
        produitDTO.setStockProduits(produit.getStockProduits().stream()
            .filter(s -> s.getStorage().getMagasin().getId().equals(magasinId))
            .map(StockProduitDTO::new).collect(Collectors.toSet()));
        produitDTO.setTotalQuantity(produitDTO.getStockProduits().stream()
            .collect(Collectors.summingInt(StockProduitDTO::getQtyStock)));
        return produitDTO;
    }

    public static ProduitDTO stockProduit(ProduitDTO produitDTO,
        StockProduit stockProduitPointOfSale) {
        if (stockProduitPointOfSale != null) {
            produitDTO.setStockProduit(new StockProduitDTO(stockProduitPointOfSale))
                .setSaleOfPointStock(stockProduitPointOfSale.getQtyStock());

        }
        return produitDTO;
    }

    public static ProduitDTO stockProduits(ProduitDTO produitDTO, Produit produit) {
        produitDTO.setStockProduits(produit.getStockProduits().stream().map(StockProduitDTO::new)
            .collect(Collectors.toSet()));
        StockProduit stockProduitPointOfSale = produit.getStockProduitPointOfSale();
        if (stockProduitPointOfSale != null) {
            produitDTO.setStockProduit(new StockProduitDTO(stockProduitPointOfSale));
            produitDTO.setSaleOfPointStock(stockProduitPointOfSale.getQtyStock());
        }
        return produitDTO;
    }

    public static ProduitDTO rayonProduits(ProduitDTO produitDTO, Produit produit, Long magasinId) {
        Set<RayonProduit> rayonProduits = produit.getRayonProduits();
        if (!CollectionUtils.isEmpty(rayonProduits)) {
            produitDTO.setRayonProduits(rayonProduits.stream().map(RayonProduitDTO::new).toList());

            Optional<RayonProduitDTO> rayon = produitDTO.getRayonProduits().stream().filter(
                r -> (r.getStorageType().equalsIgnoreCase(StorageType.PRINCIPAL.getValue())
                    && r.getMagasinId().equals(magasinId))).findFirst();
            if (rayon.isPresent()) {
                RayonProduitDTO rayonProduitDTO = rayon.get();
                produitDTO.setRayonId(rayonProduitDTO.getRayonId())
                    .setRayonLibelle(rayonProduitDTO.getLibelleRayon());

            }
        }
        return produitDTO;
    }

    public static ProduitDTO dailyStocks(ProduitDTO produitDTO, Produit produit) {
        if (!CollectionUtils.isEmpty(produit.getDailyStocks())) {
            produitDTO.dailyStocks(produit.getDailyStocks().stream()
                .sorted(Comparator.comparing(DailyStock::getStockDay, Comparator.reverseOrder()))
                .collect(Collectors.toList()));
        }
        return produitDTO;
    }

    public static ProduitDTO remiseProduit(ProduitDTO produitDTO, Produit produit) {
        RemiseProduit remiseProduit = produit.getRemise();
        if (remiseProduit != null) {
            produitDTO.setRemiseId(remiseProduit.getId());
            produitDTO.setTauxRemise(remiseProduit.getRemiseValue());

        }
        return produitDTO;
    }

    public static ProduitDTO buildFromProduit(Produit produit, Magasin magasin,
        StockProduit stockProduitPointOfSale) {
        ProduitDTO produitDTO = partialFromProduit(produit);
        laboratoire(produitDTO, produit);
        formProduit(produitDTO, produit);
        TypeEtiquette(produitDTO, produit);
        familleProduit(produitDTO, produit);
        gammeProduit(produitDTO, produit);
        tva(produitDTO, produit);
        stockProduits(produitDTO, produit, magasin.getId());
        stockProduit(produitDTO, stockProduitPointOfSale);
        rayonProduits(produitDTO, produit, magasin.getId());
        dailyStocks(produitDTO, produit);
        remiseProduit(produitDTO, produit);
        produits(produitDTO, produit);
        fournisseurProduits(produitDTO, produit);
        return produitDTO;
    }

    public static ProduitDTO produits(ProduitDTO produitDTO, Produit produit) {
        return produitDTO.setProduits(
            produit.getProduits().stream().map(ProduitBuilder::fromProduit)
                .collect(Collectors.toList()));
    }

    public static ProduitDTO fournisseurProduits(ProduitDTO produitDTO, Produit produit) {
        produitDTO.setFournisseurProduits(
            produit.getFournisseurProduits().stream().map(FournisseurProduitDTO::new)
                .collect(Collectors.toSet()));
        return produitDTO;
    }

    public static ProduitDTO partialFromProduit(Produit produit) {
        Produit parent = produit.getParent();
        ProduitDTO produitDTO = ProduitDTO.builder().id(produit.getId())
            .libelle(produit.getLibelle()).cmuAmount(produit.getCmuAmount())
            .typeProduit(produit.getTypeProduit()).costAmount(produit.getCostAmount())
            .regularUnitPrice(produit.getRegularUnitPrice()).netUnitPrice(produit.getNetUnitPrice())
            .createdAt(produit.getCreatedAt()).updatedAt(produit.getUpdatedAt())
            .itemQty(produit.getItemQty()).itemQuantity(
                produit.getProduits().stream().collect(Collectors.summingInt(Produit::getItemQty)))
            .itemCostAmount(produit.getItemCostAmount())
            .itemRegularUnitPrice(produit.getItemRegularUnitPrice())
            .produitId(Objects.nonNull(parent) ? parent.getId() : null)
            .typeProduit(produit.getTypeProduit())
            .produitLibelle(Objects.nonNull(parent) ? parent.getLibelle() : null).build();
        produitDTO.setQtyAppro(produit.getQtyAppro()).setQtySeuilMini(produit.getQtySeuilMini());
        produitDTO.setDateperemption(produit.getCheckExpiryDate());
        produitDTO.setChiffre(produit.getChiffre());
        produitDTO.setDeconditionnable(produit.getDeconditionnable())
            .setCodeEan(produit.getCodeEan());
        if (produit.getPerimeAt() != null) {
            produitDTO.expirationDate(
                produit.getPerimeAt().format(DateTimeFormatter.ofPattern(PERIME_DATE_PATERN)));
        }
        produitDTO.setStatus(produit.getStatus().ordinal());
        produitDTO.displayStatut(produit.getStatus().name());
        FournisseurProduitDTO fournisseurProduit = fromPrincipal(produit);
        if (Objects.nonNull(fournisseurProduit)) {
            produitDTO.setCodeCip(fournisseurProduit.getCodeCip())
                .setFournisseurId(fournisseurProduit.getFournisseurId())
                .setCostAmount(fournisseurProduit.getPrixAchat())
                .setRegularUnitPrice(fournisseurProduit.getPrixUni());
        }
        return produitDTO;
    }

    public static ProduitDTO rayonProduits(ProduitDTO produitDTO, Produit produit) {
        Set<RayonProduit> rayonProduits = produit.getRayonProduits();
        if (!CollectionUtils.isEmpty(rayonProduits)) {
            produitDTO.setRayonProduits(rayonProduits.stream().map(RayonProduitDTO::new).toList());

            Optional<RayonProduit> rayon = produit.getRayonProduits().stream()
                .filter(r -> r.getRayon().getStorage().getStorageType() == StorageType.PRINCIPAL)
                .findFirst();

            if (rayon.isPresent()) {
                Rayon rayonProduitDTO = rayon.get().getRayon();
                produitDTO.setRayonId(rayonProduitDTO.getId())
                    .setRayonLibelle(rayonProduitDTO.getLibelle());

            }
        }
        return produitDTO;
    }

    public static ProduitDTO fromProduit(Produit produit) {
        ProduitDTO produitDTO = partialFromProduit(produit);
        fournisseurProduits(produitDTO, produit);
        laboratoire(produitDTO, produit);
        formProduit(produitDTO, produit);
        TypeEtiquette(produitDTO, produit);
        familleProduit(produitDTO, produit);
        gammeProduit(produitDTO, produit);
        tva(produitDTO, produit);
        stockProduits(produitDTO, produit);
        rayonProduits(produitDTO, produit);
        dailyStocks(produitDTO, produit);
        remiseProduit(produitDTO, produit);
        produits(produitDTO, produit);
        produitDTO.setStatus(produit.getStatus().ordinal());
        produitDTO.setDisplayField(
            String.format("%s %s %d", produitDTO.getCodeCip(), produitDTO.getLibelle(),
                produitDTO.getRegularUnitPrice()));

        return produitDTO;
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
            dto.setSaleOfPointStock(stockProduitPointOfSale.getQtyStock());
            dto.setSaleOfPointVirtualStock(stockProduitPointOfSale.getQtyVirtual());
            try {
                Rayon rayon = produit.getRayonProduits().stream().filter(
                        r -> r.getRayon().getStorage().getStorageType() == StorageType.PRINCIPAL)
                    .findFirst().get().getRayon();
                dto.setRayonId(rayon.getId());
                dto.setRayonLibelle(rayon.getLibelle());
            } catch (Exception e) {

            }
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
        //

        return dto;
    }

    public static RemiseProduit resmiseProduitFromId(Long id) {
        if (id == null) {
            return null;
        }
        RemiseProduit remiseProduit = new RemiseProduit();
        remiseProduit.setId(id);
        return remiseProduit;
    }

    public static Tva tvaFromId(Long tvaId) {
        if (tvaId == null) {
            return null;
        }
        Tva tva = new Tva();
        tva.setId(tvaId);
        return tva;
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

    public static StockProduit stockProduitFromProduitDTO(Storage storage) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setQtyStock(0);
        stockProduit.setQtyVirtual(0);
        stockProduit.setCreatedAt(Instant.now());
        stockProduit.setUpdatedAt(stockProduit.getCreatedAt());
        stockProduit.setQtyUG(0);
        stockProduit.setStorage(storage);
        return stockProduit;
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

    public static FournisseurProduitDTO fromPrincipal(Produit produit) {
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        log.info(fournisseurProduit.getCodeCip());

        return Optional.ofNullable(fournisseurProduit).map(FournisseurProduitDTO::new).orElse(null);
    }

    public static Produit buildProduitFromProduitDTO(ProduitDTO produitDTO, Produit produit) {
        produit.setUpdatedAt(Instant.now());
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCostAmount(produitDTO.getCostAmount());
        produit.setCmuAmount(produitDTO.getCmuAmount());
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
        produit.setCodeEan(produitDTO.getCodeEan());
        produit.setCheckExpiryDate(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());
        if (StringUtils.isNotEmpty(produitDTO.getExpirationDate())) {
            produit.setPerimeAt(LocalDate.parse(produitDTO.getExpirationDate(),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        produit.setRemise(resmiseProduitFromId(produitDTO.getRemiseId()));
        produit.setTva(tvaFromId(produitDTO.getTvaId()));
        produit.setLaboratoire(laboratoireFromId(produitDTO.getLaboratoireId()));
        produit.setFamille(familleProduitFromId(produitDTO.getFamilleId()));
        produit.setGamme(gammeFromId(produitDTO.getGammeId()));
        produit.setTypeEtyquette(typeEtiquetteFromId(produitDTO.getTypeEtiquetteId()));
        produit.setForme(formProduitFromId(produitDTO.getFormeId()));
        produit.addFournisseurProduit(fournisseurProduitProduit(produit, produitDTO));
        return produit;
    }

    public static FournisseurProduit fournisseurProduitProduit(Produit produit, ProduitDTO dto) {
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        fournisseurProduit.setUpdatedAt(Instant.now());
        fournisseurProduit.setCodeCip(dto.getCodeCip());
        fournisseurProduit.setPrixAchat(dto.getCostAmount());
        fournisseurProduit.setPrixUni(dto.getRegularUnitPrice());
        fournisseurProduit.setProduit(produit);
        return fournisseurProduit;
    }

}
