package com.kobe.warehouse.service.remise.impl;

import static com.kobe.warehouse.service.remise.impl.JournalRemisesSpecifications.calculerPartPromotion;
import static com.kobe.warehouse.service.remise.impl.JournalRemisesSpecifications.joindreCaissier;
import static com.kobe.warehouse.service.remise.impl.JournalRemisesSpecifications.joindreClient;
import static com.kobe.warehouse.service.remise.impl.JournalRemisesSpecifications.joindreFamille;
import static com.kobe.warehouse.service.remise.impl.JournalRemisesSpecifications.joindreFournisseurProduit;
import static com.kobe.warehouse.service.remise.impl.JournalRemisesSpecifications.joindreGrille;
import static com.kobe.warehouse.service.remise.impl.JournalRemisesSpecifications.joindreProduit;
import static com.kobe.warehouse.service.remise.impl.JournalRemisesSpecifications.joindreVendeur;
import static com.kobe.warehouse.service.remise.impl.JournalRemisesSpecifications.joindreVente;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.FamilleProduit_;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.Remise_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.service.remise.dto.JournalRemisesFiltre;
import com.kobe.warehouse.service.remise.dto.NatureJournal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Les lignes du journal, lues en une requête dont seuls les critères changent
 * ({@link JournalRemisesSpecifications}). Chaque colonne porte un nom ({@link Colonne}) :
 * {@link LignesJournal} la lit par ce nom, jamais par sa position.
 */
@Repository
public class JournalRemisesRequetes {

    private static final int TAILLE_LOT = 500;

    @PersistenceContext
    private EntityManager entityManager;

    /** Les noms des colonnes lues. */
    static final class Colonne {

        static final String DATE = "date";
        static final String HEURE = "heure";
        static final String TICKET = "ticket";
        static final String NATURE_VENTE = "natureVente";
        static final String CAISSIER_PRENOM = "caissierPrenom";
        static final String CAISSIER_NOM = "caissierNom";
        static final String VENDEUR_PRENOM = "vendeurPrenom";
        static final String VENDEUR_NOM = "vendeurNom";
        static final String CLIENT_PRENOM = "clientPrenom";
        static final String CLIENT_NOM = "clientNom";
        static final String CIP = "cip";
        static final String PRODUIT = "produit";
        static final String FAMILLE = "famille";
        static final String QUANTITE = "quantite";
        static final String PRIX_UNITAIRE = "prixUnitaire";
        static final String MONTANT_BRUT = "montantBrut";
        static final String REMISE = "remise";
        static final String PART_PROMOTION = "partPromotion";
        static final String PROMOTION_ID = "promotionId";
        static final String PROMOTION_LIBELLE = "promotionLibelle";
        static final String UNITES_PROMO = "unitesPromo";
        static final String GRILLE = "grille";
        static final String QUANTITE_UG = "quantiteUg";
        static final String PRIX_ACHAT = "prixAchat";
        static final String TAUX_TVA = "tauxTva";

        private Colonne() {}
    }

    /**
     * Toutes les lignes d'une nature, en flux : pour la synthèse et les exports.
     *
     * @param nature nulle pour toutes les lignes d'au moins une nature ; le filtre d'origine est
     *     alors ignoré, la synthèse l'applique elle-même aux seules remises
     */
    Stream<Tuple> lire(NatureJournal nature, JournalRemisesFiltre filtre, Integer magasinId) {
        return construireRequete(nature, filtre, magasinId)
                .setHint(HINT_FETCH_SIZE, TAILLE_LOT)
                .setHint(HINT_READ_ONLY, true)
                .getResultStream();
    }

    /** Une page de lignes d'une nature, dans l'ordre du journal. */
    List<Tuple> lirePage(NatureJournal nature, JournalRemisesFiltre filtre, Integer magasinId, int debut, int taille) {
        return construireRequete(nature, filtre, magasinId)
                .setFirstResult(debut)
                .setMaxResults(taille)
                .setHint(HINT_READ_ONLY, true)
                .getResultList();
    }

    long compter(NatureJournal nature, JournalRemisesFiltre filtre, Integer magasinId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<SalesLine> ligne = query.from(SalesLine.class);
        Specification<SalesLine> criteres = JournalRemisesSpecifications.composer(nature, filtre, magasinId);
        query.select(cb.count(ligne)).where(criteres.toPredicate(ligne, query, cb));
        return entityManager.createQuery(query).getSingleResult();
    }

    /** Les colonnes du journal, ses critères et son ordre : par jour, heure, ticket, produit. */
    private TypedQuery<Tuple> construireRequete(NatureJournal nature, JournalRemisesFiltre filtre, Integer magasinId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<SalesLine> ligne = query.from(SalesLine.class);
        Join<SalesLine, Sales> vente = joindreVente(ligne);
        Join<SalesLine, Produit> produit = joindreProduit(ligne);
        Join<Sales, AppUser> caissier = joindreCaissier(ligne);
        Join<Sales, AppUser> vendeur = joindreVendeur(ligne);
        Join<Sales, Customer> client = joindreClient(ligne);

        query.select(cb.tuple(
                vente.get(Sales_.saleDate).alias(Colonne.DATE),
                vente.get(Sales_.updatedAt).alias(Colonne.HEURE),
                vente.get(Sales_.numberTransaction).alias(Colonne.TICKET),
                vente.get(Sales_.natureVente).alias(Colonne.NATURE_VENTE),
                caissier.get(AppUser_.firstName).alias(Colonne.CAISSIER_PRENOM),
                caissier.get(AppUser_.lastName).alias(Colonne.CAISSIER_NOM),
                vendeur.get(AppUser_.firstName).alias(Colonne.VENDEUR_PRENOM),
                vendeur.get(AppUser_.lastName).alias(Colonne.VENDEUR_NOM),
                client.get(Customer_.firstName).alias(Colonne.CLIENT_PRENOM),
                client.get(Customer_.lastName).alias(Colonne.CLIENT_NOM),
                joindreFournisseurProduit(ligne)
                        .get(FournisseurProduit_.codeCip)
                        .alias(Colonne.CIP),
                produit.get(Produit_.libelle).alias(Colonne.PRODUIT),
                joindreFamille(ligne).get(FamilleProduit_.libelle).alias(Colonne.FAMILLE),
                ligne.get(SalesLine_.quantityRequested).alias(Colonne.QUANTITE),
                ligne.get(SalesLine_.regularUnitPrice).alias(Colonne.PRIX_UNITAIRE),
                ligne.get(SalesLine_.salesAmount).alias(Colonne.MONTANT_BRUT),
                ligne.get(SalesLine_.discountAmount).alias(Colonne.REMISE),
                calculerPartPromotion(ligne, cb).alias(Colonne.PART_PROMOTION),
                ligne.get(SalesLine_.promotionId).alias(Colonne.PROMOTION_ID),
                ligne.get(SalesLine_.promotionLibelle).alias(Colonne.PROMOTION_LIBELLE),
                ligne.get(SalesLine_.quantitePromo).alias(Colonne.UNITES_PROMO),
                joindreGrille(ligne).get(Remise_.valeur).alias(Colonne.GRILLE),
                ligne.get(SalesLine_.quantityUg).alias(Colonne.QUANTITE_UG),
                ligne.get(SalesLine_.costAmount).alias(Colonne.PRIX_ACHAT),
                ligne.get(SalesLine_.taxValue).alias(Colonne.TAUX_TVA)));

        query.where(
                JournalRemisesSpecifications.composer(nature, filtre, magasinId).toPredicate(ligne, query, cb));
        query.orderBy(
                cb.asc(vente.get(Sales_.saleDate)),
                cb.asc(vente.get(Sales_.updatedAt)),
                cb.asc(vente.get(Sales_.numberTransaction)),
                cb.asc(produit.get(Produit_.libelle)));
        return entityManager.createQuery(query);
    }
}
