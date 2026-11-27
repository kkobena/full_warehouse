package com.kobe.warehouse.service.remise.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.FamilleProduit_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.remise.dto.JournalRemisesFiltre;
import com.kobe.warehouse.service.remise.dto.NatureJournal;
import com.kobe.warehouse.service.remise.dto.OrigineRemise;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/**
 * Les critères du journal, un par Specification, sur les lignes de vente.
 *
 * <p>Le périmètre est celui du bilan d'une promotion : ventes clôturées, non annulées, du chiffre
 * d'affaires (une vente dépôt est un transfert), du magasin, datées de la période. Une vente annulée
 * sort donc du journal, et sa contre-passation (statut {@code CANCELED}) aussi.
 *
 * <p>Les jointures passent par {@code joindre…} : une Specification réutilise celle que la
 * projection a déjà posée au lieu d'en ajouter une seconde, qui multiplierait les lignes.
 */
final class JournalRemisesSpecifications {

    private JournalRemisesSpecifications() {}

    /** Le périmètre, la nature et les filtres retenus ; un filtre vide ne restreint rien. */
    static Specification<SalesLine> composer(NatureJournal nature, JournalRemisesFiltre filtre, Integer magasinId) {
        List<Specification<SalesLine>> criteres = new ArrayList<>();
        criteres.add(limiterAuPerimetre(magasinId, filtre.du(), filtre.au()));
        criteres.add(retenirLaNature(nature));
        if (!filtre.caissiers().isEmpty()) {
            criteres.add(retenirLesCaissiers(filtre.caissiers()));
        }
        if (!filtre.vendeurs().isEmpty()) {
            criteres.add(retenirLesVendeurs(filtre.vendeurs()));
        }
        if (!filtre.naturesVente().isEmpty()) {
            criteres.add(retenirLesNaturesVente(filtre.naturesVente()));
        }
        if (!filtre.promotions().isEmpty()) {
            criteres.add(retenirLesPromotions(filtre.promotions()));
        }
        if (!filtre.familles().isEmpty()) {
            criteres.add(retenirLesFamilles(filtre.familles()));
        }
        // L'origine qualifie une remise : elle ne filtre ni le journal des UG ni la lecture de la synthèse.
        if (!filtre.origines().isEmpty() && (nature == NatureJournal.PROMOTIONS || nature == NatureJournal.REMISES)) {
            criteres.add(retenirLesOrigines(filtre.origines()));
        }
        return Specification.allOf(criteres);
    }

    // ===== critères =====
    // Un filtre sur un identifiant lit la clé étrangère (s.caissier_id, p.famille_id) : il n'ajoute
    // aucune jointure au comptage.

    /**
     * Le périmètre porte sur la vente. La borne basse est répétée sur la ligne, clé de partition de
     * {@code sales_line}, pour que PostgreSQL écarte les partitions antérieures à la période : la
     * jointure se fait sur {@code sales_sale_date}, qui n'est pas cette clé. Elle ne retire aucune
     * ligne, car une ligne est datée de sa création, qui suit toujours celle de sa vente. La borne
     * haute ne se répète pas : une prévente ouverte la veille porte des lignes du lendemain.
     */
    static Specification<SalesLine> limiterAuPerimetre(Integer magasinId, LocalDate du, LocalDate au) {
        return (ligne, query, cb) -> {
            Join<SalesLine, Sales> vente = joindreVente(ligne);
            return cb.and(
                    cb.greaterThanOrEqualTo(ligne.get(SalesLine_.saleDate), du),
                    cb.equal(vente.get(Sales_.magasin).get(Magasin_.id), magasinId),
                    cb.between(vente.get(Sales_.saleDate), du, au),
                    cb.equal(vente.get(Sales_.statut), SalesStatut.CLOSED),
                    cb.isFalse(vente.get(Sales_.canceled)),
                    cb.equal(vente.get(Sales_.categorieChiffreAffaire), CategorieChiffreAffaire.CA));
        };
    }

    /** Nulle : toute ligne d'au moins une nature — promue, remisée ou porteuse d'UG. */
    static Specification<SalesLine> retenirLaNature(NatureJournal nature) {
        return (ligne, query, cb) -> {
            if (nature == null) {
                return cb.or(
                        cb.isNotNull(ligne.get(SalesLine_.promotionId)),
                        cb.notEqual(calculerRemise(ligne, cb), 0),
                        cb.gt(ligne.get(SalesLine_.quantityUg), 0));
            }
            return switch (nature) {
                case PROMOTIONS -> cb.isNotNull(ligne.get(SalesLine_.promotionId));
                case REMISES -> cb.notEqual(calculerPartRemiseProduit(ligne, cb), 0);
                case UG -> cb.gt(ligne.get(SalesLine_.quantityUg), 0);
            };
        };
    }

    static Specification<SalesLine> retenirLesCaissiers(Set<Integer> caissiers) {
        return (ligne, query, cb) ->
                joindreVente(ligne).get(Sales_.caissier).get(AppUser_.id).in(caissiers);
    }

    static Specification<SalesLine> retenirLesVendeurs(Set<Integer> vendeurs) {
        return (ligne, query, cb) ->
                joindreVente(ligne).get(Sales_.seller).get(AppUser_.id).in(vendeurs);
    }

    static Specification<SalesLine> retenirLesNaturesVente(Set<NatureVente> natures) {
        return (ligne, query, cb) -> joindreVente(ligne).get(Sales_.natureVente).in(natures);
    }

    static Specification<SalesLine> retenirLesPromotions(Set<Long> promotions) {
        return (ligne, query, cb) -> ligne.get(SalesLine_.promotionId).in(promotions);
    }

    static Specification<SalesLine> retenirLesFamilles(Set<Integer> familles) {
        return (ligne, query, cb) -> joindreProduit(ligne)
                .get(Produit_.famille)
                .get(FamilleProduit_.id)
                .in(familles);
    }

    /** Une ligne d'au moins une des origines retenues. */
    static Specification<SalesLine> retenirLesOrigines(Set<OrigineRemise> origines) {
        return Specification.anyOf(origines.stream()
                .map(JournalRemisesSpecifications::retenirLOrigine)
                .toList());
    }

    static Specification<SalesLine> retenirLOrigine(OrigineRemise origine) {
        return (ligne, query, cb) -> {
            Path<Long> promotion = ligne.get(SalesLine_.promotionId);
            return switch (origine) {
                case PROMOTION -> cb.and(cb.isNotNull(promotion), cb.equal(calculerPartRemiseProduit(ligne, cb), 0));
                case REMISE_PRODUIT -> cb.and(cb.isNull(promotion), cb.notEqual(calculerRemise(ligne, cb), 0));
                case CUMUL -> cb.and(cb.isNotNull(promotion), cb.notEqual(calculerPartRemiseProduit(ligne, cb), 0));
            };
        };
    }

    // ===== montants calculés =====

    /** La remise de la ligne, zéro si elle n'en porte pas. */
    static Expression<Integer> calculerRemise(Root<SalesLine> ligne, CriteriaBuilder cb) {
        return cb.coalesce(ligne.get(SalesLine_.discountAmount), 0);
    }

    /**
     * Part de la promotion : celle que le moteur a enregistrée, ou toute la remise d'une ligne promue
     * enregistrée avant que la part ne soit tenue (V2.1.68, sans reprise).
     */
    static Expression<Integer> calculerPartPromotion(Root<SalesLine> ligne, CriteriaBuilder cb) {
        Path<Integer> partEnregistree = ligne.get(SalesLine_.remisePromotion);
        return cb.<Integer>selectCase()
                .when(cb.isNull(ligne.get(SalesLine_.promotionId)), cb.literal(0))
                .when(cb.notEqual(partEnregistree, 0), partEnregistree)
                .otherwise(calculerRemise(ligne, cb));
    }

    /** Ce qui, dans la remise, revient à la grille du produit. */
    static Expression<Integer> calculerPartRemiseProduit(Root<SalesLine> ligne, CriteriaBuilder cb) {
        return cb.diff(calculerRemise(ligne, cb), calculerPartPromotion(ligne, cb));
    }

    // ===== jointures =====

    static Join<SalesLine, Sales> joindreVente(Root<SalesLine> ligne) {
        return joindre(ligne, SalesLine_.sales, JoinType.INNER);
    }

    static Join<SalesLine, Produit> joindreProduit(Root<SalesLine> ligne) {
        return joindre(ligne, SalesLine_.produit, JoinType.INNER);
    }

    static Join<Produit, FamilleProduit> joindreFamille(Root<SalesLine> ligne) {
        return joindre(joindreProduit(ligne), Produit_.famille, JoinType.LEFT);
    }

    static Join<Produit, FournisseurProduit> joindreFournisseurProduit(Root<SalesLine> ligne) {
        return joindre(joindreProduit(ligne), Produit_.fournisseurProduitPrincipal, JoinType.LEFT);
    }

    static Join<Sales, AppUser> joindreCaissier(Root<SalesLine> ligne) {
        return joindre(joindreVente(ligne), Sales_.caissier, JoinType.LEFT);
    }

    static Join<Sales, AppUser> joindreVendeur(Root<SalesLine> ligne) {
        return joindre(joindreVente(ligne), Sales_.seller, JoinType.LEFT);
    }

    static Join<Sales, Customer> joindreClient(Root<SalesLine> ligne) {
        return joindre(joindreVente(ligne), Sales_.customer, JoinType.LEFT);
    }

    static Join<Sales, Remise> joindreGrille(Root<SalesLine> ligne) {
        return joindre(joindreVente(ligne), Sales_.remise, JoinType.LEFT);
    }

    /** La jointure déjà posée sur cet attribut, ou une nouvelle. */
    @SuppressWarnings("unchecked")
    private static <X, Y> Join<X, Y> joindre(
            From<?, X> depuis, SingularAttribute<? super X, Y> attribut, JoinType type) {
        for (Join<X, ?> jointure : depuis.getJoins()) {
            if (jointure.getAttribute().getName().equals(attribut.getName()) && jointure.getJoinType() == type) {
                return (Join<X, Y>) jointure;
            }
        }
        return depuis.join(attribut, type);
    }
}
