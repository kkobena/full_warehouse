package com.kobe.warehouse.repository;


import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public interface CustomizedProductService {
    List<ProduitDTO> findAll(ProduitCriteria produitCriteria) throws Exception;

    Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable) throws Exception;

    Optional<ProduitDTO> findOneById(Long ProduitId);

    void save(ProduitDTO dto, Rayon rayon) throws Exception;

    void save(Produit dto) throws Exception;

    void update(ProduitDTO dto) throws Exception;

    void save(StockProduitDTO dto) throws Exception;

    void update(StockProduitDTO dto) throws Exception;

    void save(FournisseurProduitDTO dto) throws Exception;

    void update(FournisseurProduitDTO dto) throws Exception;

    void removeFournisseurProduit(Long id) throws Exception;

    default FournisseurProduit buildFournisseurProduitFromFournisseurProduitDTO(FournisseurProduitDTO dto) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setCodeCip(dto.getCodeCip());
        fournisseurProduit.setCreatedAt(Instant.now());
        fournisseurProduit.setUpdatedAt(Instant.now());
        fournisseurProduit.setPrixAchat(dto.getPrixAchat());
        fournisseurProduit.setPrixUni(dto.getPrixUni());
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        fournisseurProduit.setPrincipal(dto.isPrincipal());
        return fournisseurProduit;
    }

    default Fournisseur fournisseurFromId(Long id) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(id);
        return fournisseur;
    }

    default FournisseurProduit buildFournisseurProduitFromFournisseurProduitDTO(FournisseurProduitDTO dto,
                                                                                FournisseurProduit fournisseurProduit) {
        fournisseurProduit.setCodeCip(dto.getCodeCip());
        fournisseurProduit.setUpdatedAt(Instant.now());
        fournisseurProduit.setPrixAchat(dto.getPrixAchat());
        fournisseurProduit.setPrixUni(dto.getPrixUni());
        fournisseurProduit.setPrincipal(dto.isPrincipal());
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        return fournisseurProduit;
    }

    default StockProduit buildStockProduitFromStockProduitDTO(StockProduitDTO dto, StockProduit stockProduit) {
        stockProduit.setQtyStock(dto.getQtyStock());
        stockProduit.setUpdatedAt(Instant.now());
        stockProduit.setQtyVirtual(dto.getQtyVirtual());
        stockProduit.setQtyUG(dto.getQtyUG());
        stockProduit.setStorage(storageFromId(dto.getStorageId()));
        return stockProduit;
    }

    default StockProduit buildStockProduitFromStockProduitDTO(StockProduitDTO dto) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setCreatedAt(Instant.now());
        stockProduit.setQtyStock(dto.getQtyStock());
        stockProduit.setUpdatedAt(Instant.now());
        stockProduit.setQtyVirtual(dto.getQtyVirtual());
        stockProduit.setQtyUG(dto.getQtyUG());
        stockProduit.setStorage(storageFromId(dto.getStorageId()));
        return stockProduit;
    }

    default Storage storageFromId(Long id) {
        if (id == null) {
            return null;
        }
        Storage storage = new Storage();
        storage.setId(id);
        return storage;

    }

    default Rayon rayonFromId(Long id) {
        if (id == null) {
            return null;
        }
        Rayon rayon = new Rayon();
        rayon.setId(id);
        return rayon;

    }

    default Magasin magasinFromId(Long id) {
        if (id == null) {
            return null;
        }
        Magasin magasin = new Magasin();
        magasin.setId(id);
        return magasin;

    }

    default Laboratoire laboratoireFromId(Long id) {
        if (id == null) {
            return null;
        }
        Laboratoire laboratoire = new Laboratoire();
        laboratoire.setId(id);
        return laboratoire;

    }

    default FormProduit formeFromId(Long id) {
        if (id == null) {
            return null;
        }
        FormProduit formProduit = new FormProduit();
        formProduit.setId(id);
        return formProduit;

    }

    default Produit parentFromId(Long id) {
        if (id == null) {
            return null;
        }
        Produit produit = new Produit();
        produit.setId(id);
        return produit;

    }

    default TypeEtiquette typeEtiquetteFromId(Long id) {
        if (id == null) {
            return null;
        }
        TypeEtiquette typeEtiquette = new TypeEtiquette();
        typeEtiquette.setId(id);
        return typeEtiquette;

    }

    default FamilleProduit familleFromId(Long id) {
        if (id == null) {
            return null;
        }
        FamilleProduit famille = new FamilleProduit();
        famille.setId(id);
        return famille;

    }

    default RemiseProduit remiseProduitFromId(Long id) {
        if (id == null) {
            return null;
        }
        RemiseProduit remiseProduit = new RemiseProduit();
        remiseProduit.setId(id);
        return remiseProduit;

    }

    default GammeProduit gammeFromId(Long id) {
        if (id == null) {
            return null;
        }
        GammeProduit famille = new GammeProduit();
        famille.setId(id);
        return famille;

    }

    default Tva tvaFromId(Long id) {
        if (id == null) {
            return null;
        }
        Tva tva = new Tva();
        tva.setId(id);
        return tva;

    }

    default Produit buildProduitFromProduitDTO(ProduitDTO produitDTO, Produit produit) {
        produit.setUpdatedAt(Instant.now());
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
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCodeEan(produitDTO.getCodeEan());
        produit.setDateperemption(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());
        if (StringUtils.isNotEmpty(produitDTO.getExpirationDate())) {
            produit.setPerimeAt(LocalDate.parse(produitDTO.getExpirationDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        produit.setRemise(remiseProduitFromId(produitDTO.getRemiseId()));
        produit.setTva(tvaFromId(produitDTO.getTvaId()));
        produit.setLaboratoire(laboratoireFromId(produitDTO.getLaboratoireId()));
        produit.setFamille(familleFromId(produitDTO.getFamilleId()));
        produit.setGamme(gammeFromId(produitDTO.getRemiseId()));
        produit.setTypeEtyquette(typeEtiquetteFromId(produitDTO.getTypeEtyquetteId()));
        produit.setForme(formeFromId(produitDTO.getFormeId()));
        //   produit.addStockProduit(stockProduitFromProduitDTO(produitDTO));
        produit.addFournisseurProduit(fournisseurProduitProduit(produit, produitDTO));
        return produit;
    }

    FournisseurProduit fournisseurProduitFromDTO(ProduitDTO dto);

    FournisseurProduit fournisseurProduitProduit(Produit produit, ProduitDTO dto);

    StockProduit stockProduitFromProduitDTO(ProduitDTO dto);


    List<ProduitDTO> lite(String query);

    SalesLine lastSale(ProduitCriteria produitCriteria);

    StoreInventoryLine lastInventory(ProduitCriteria produitCriteria);

    OrderLine lastOrder(ProduitCriteria produitCriteria);

    void updateDetail(ProduitDTO dto) throws Exception;

    default Produit buildDetailProduitFromProduitDTO(ProduitDTO produitDTO, Produit produit) {
        produit.setUpdatedAt(Instant.now());
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCostAmount(produitDTO.getCostAmount());
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        //   produit.addStockProduit(stockProduitFromProduitDTO(produitDTO));
        //  produit.addFournisseurProduit(fournisseurProduitFromDTO(produitDTO));
        return produit;
    }

}
