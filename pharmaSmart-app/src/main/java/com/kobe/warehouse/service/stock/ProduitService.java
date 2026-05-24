package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.enumeration.ProduitFlag;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.SubstitutDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.Produit}.
 */
public interface ProduitService {
    /**
     * Save a produit.
     *
     * @param produitDTO the entity to save.
     * @return the id of the created produit.
     */
    Long save(ProduitDTO produitDTO);

    /**
     * Save a produit detail.
     *
     * @param dto the entity to save.
     * @return the id of the created produit.
     */
    Long saveDetail(ProduitDTO dto);

    /**
     * Get all the produits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ProduitDTO> findAll(Pageable pageable);

    /**
     * Get the "id" produit.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ProduitDTO> findOne(Integer id);

    /**
     * Delete the "id" produit.
     *
     * @param id the id of the entity.
     */
    void delete(Integer id);

    Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable);

    LocalDateTime lastSale(ProduitCriteria produitCriteria);

    LocalDateTime lastOrder(ProduitCriteria produitCriteria);

    ProduitDTO findOne(ProduitCriteria produitCriteria);

    List<ProduitDTO> findWithCriteria(ProduitCriteria produitCriteria);

    void update(ProduitDTO produitDTO);

    List<ProduitDTO> productsLiteList(ProduitCriteria produitCriteria, Pageable pageable);

    void updateDetail(ProduitDTO produitDTO);

    int getProductTotalStock(Integer productId);

    StockProduit updateTotalStock(Produit produit, int stockIn, int stockUg);

    void update(Produit produit);
    //TODO: methode à analyser , ça ne me semble pas être la bonne formule pour calculer le prix moyen pondéré à la réception d'une commande
    default int calculPrixMoyenPondereReception(int oldStock, int oldPrixAchat, int newStock, int newPrixAchat) {

       // return ((oldStock * oldPrixAchat) + (newStock * newPrixAchat)) / (oldStock + newStock);
        return ((oldStock * oldPrixAchat) + (newStock * newPrixAchat)) / newStock;
    }
    int produitTotalStock(Produit produit);
    void updateFromCommande(ProduitDTO produitDTO, Produit produit);

    Produit findReferenceById(Integer id);

    List<ProduitSearch> searchProducts(String search, Integer magasinId, Pageable pageable);

    List<ProduitSearch> searchProductsByStorage(@NotNull Integer storageId, String search, Pageable pageable);

    // saveDetail avec retour d'ID — voir déclaration en haut de l'interface

    void save(StockProduitDTO dto);
    Optional<FournisseurProduit> getFournisseurProduitByCriteria(String criteteria, Integer fournisseurId);
    List<Produit> find(ProduitCriteria produitCriteria);
    List<Produit> findByIds(Set<Integer> ids);
    Optional<Produit> findProduitById(Integer id);
    Produit updateProduit(Produit produit);
    void deleteProduit(Integer id);
    void changeStatus(Integer id, com.kobe.warehouse.domain.enumeration.Status status);

    void toggleGestionLot(Integer id, boolean active);

    void toggleFlag(Integer id, ProduitFlag flag, boolean value);

    List<SubstitutDTO> findGeneriques(Integer produitId);
}
