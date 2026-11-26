package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.AvoirClient_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public interface AvoirClientRepository
        extends JpaRepository<AvoirClient, Integer>, JpaSpecificationExecutor<AvoirClient> {

    static Specification<AvoirClient> forCommande(Set<Integer> produitIds) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(AvoirClient_.statut), AvoirClientStatut.OUVERT),
                root.get(AvoirClient_.produit).get(Produit_.id).in(produitIds),
                root.get(AvoirClient_.commande).isNull());
    }

    static Specification<AvoirClient> buildSpec(
            String search, LocalDate fromDate, LocalDate toDate, AvoirClientStatut statut) {
        Specification<AvoirClient> spec = Specification.unrestricted();
        Set<AvoirClientStatut> statuts = Set.of(AvoirClientStatut.OUVERT, AvoirClientStatut.CLOTURE);
        if (statut != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(AvoirClient_.statut), statut));
        } else {
            spec = spec.and((root, query, cb) -> root.get(AvoirClient_.statut).in(statuts));
        }
        if (fromDate != null && toDate != null) {
            spec = spec.and((root, query, cb) -> cb.between(
                    root.get(AvoirClient_.createdAt),
                    fromDate.atStartOfDay(),
                    toDate.plusDays(1).atStartOfDay()));
        }
        if (StringUtils.hasText(search)) {
            String term = "%" + search.toUpperCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.upper(root.get(AvoirClient_.produit).get(Produit_.libelle)), term),
                    cb.like(cb.upper(root.get(AvoirClient_.reference)), term),
                    cb.like(
                            cb.upper(root.join(AvoirClient_.salesLine, JoinType.LEFT)
                                    .join(SalesLine_.sales, JoinType.LEFT)
                                    .get(Sales_.numberTransaction)),
                            term)));
        }
        return spec;
    }

    Optional<AvoirClient> findBySalesLineId(Long salesLineId);

    boolean existsByStatutAndCommandeIsNull(AvoirClientStatut statut);

    List<AvoirClient> findByStatutAndDateExpirationLessThanEqual(AvoirClientStatut statut, LocalDate date);

    List<AvoirClient> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    /**
     * Nombre d avoirs ouverts et reste a servir : une seule ligne, rendue dans une liste.
     */
    @Query(
            "SELECT COUNT(a), COALESCE(SUM(a.montant - a.montantUtilise), 0) FROM AvoirClient a WHERE a.statut = 'OUVERT'")
    List<Object[]> statsAvoirsOuverts();

    @Query(
            "SELECT COUNT(a) FROM AvoirClient a WHERE a.statut = 'OUVERT' AND a.dateExpiration IS NOT NULL AND a.dateExpiration < :seuil")
    long countAvoirsProchesExpiration(
            @org.springframework.data.repository.query.Param("seuil") java.time.LocalDate seuil);

    /**
     * Produits promis a des clients qu'aucune commande partie chez le grossiste ne couvre.
     *
     * <p>Le NOT EXISTS definit ce que « deja commande » veut dire : une ligne dans une commande
     * EN ATTENTE ET DATEE D'UN ENVOI. Un brouillon ne couvre rien — il n'est pas parti, le
     * grossiste ne l'a jamais vu, et le compter reviendrait a faire disparaitre de cette liste les
     * produits qu'on a justement oublie d'envoyer. C'est ce que la date d'envoi rend possible :
     * avant elle, toute commande en attente passait pour transmise.
     *
     * <p>quantite - quantiteRemise : un avoir partiellement servi reste du pour le solde.
     */
    @Query("""
            SELECT a.produit.id, a.produit.libelle, a.produit.codeEanLaboratoire,
                   COUNT(DISTINCT a.customer.id), COUNT(a),
                   SUM(a.quantite - a.quantiteRemise), MIN(a.createdAt),
                   (SELECT COALESCE(SUM(sp.qtyStock), 0) FROM StockProduit sp WHERE sp.produit.id = a.produit.id)
            FROM AvoirClient a
            WHERE a.statut = com.kobe.warehouse.domain.enumeration.AvoirClientStatut.OUVERT
              AND a.commande IS NULL
              AND a.quantite > a.quantiteRemise
              AND NOT EXISTS (
                SELECT 1 FROM OrderLine ol
                WHERE ol.fournisseurProduit.produit.id = a.produit.id
                  AND ol.commande.orderStatus = com.kobe.warehouse.domain.enumeration.OrderStatut.REQUESTED
                  AND ol.commande.dateEnvoi IS NOT NULL
              )
            GROUP BY a.produit.id, a.produit.libelle, a.produit.codeEanLaboratoire
            ORDER BY MIN(a.createdAt)
            """)
    List<Object[]> lireProduitsDusNonCommandes();

    /**
     * Unites dues a des clients sur un produit : les avoirs ouverts, deduction faite de ce qui a
     * deja ete remis.
     *
     * <p>Ce stock est physiquement present mais reserve. Le retrancher du stock disponible est ce
     * qui empeche le reassort de croire a une couverture qui n'existe pas.
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(SUM(a.quantite - a.quantiteRemise), 0)
            FROM AvoirClient a
            WHERE a.statut = com.kobe.warehouse.domain.enumeration.AvoirClientStatut.OUVERT
              AND a.quantite > a.quantiteRemise
              AND a.produit.id = :produitId
            """)
    long lireDetteOuverte(@org.springframework.data.repository.query.Param("produitId") Integer produitId);

    /**
     * Clients en attente, par produit, pour une poignee de produits affiches a l'ecran.
     *
     * <p>Contrairement a la requete precedente, celle-ci ne retire PAS les produits deja
     * commandes : « 3 clients attendent ce produit » reste vrai pendant que le bon est en route.
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT a.produit.id, COUNT(DISTINCT a.customer.id), COUNT(a), SUM(a.quantite - a.quantiteRemise)
            FROM AvoirClient a
            WHERE a.statut = com.kobe.warehouse.domain.enumeration.AvoirClientStatut.OUVERT
              AND a.quantite > a.quantiteRemise
              AND a.produit.id IN :produitIds
            GROUP BY a.produit.id
            """)
    List<Object[]> lireClientsEnAttente(
            @org.springframework.data.repository.query.Param("produitIds") Set<Integer> produitIds);
}
