package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.PilotageVenteJour;
import com.kobe.warehouse.domain.PilotageVenteLigneJour;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.MesuresVentileesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteVentilationDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Requête de ventilation construite selon les axes demandés (Criteria) : un axe ajoute sa jointure au référentiel, partagée avec
 * les filtres qui portent sur le même axe. On groupe par la clé seule ; le libellé, unique par clé, est lu par {@code max}.
 */
@Repository
public class PilotageAnalyseRepositoryCustomImpl implements PilotageAnalyseRepositoryCustom {

    private final EntityManager em;

    public PilotageAnalyseRepositoryCustomImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<MesuresVentileesDTO> ventiler(RequeteVentilationDTO requete) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Class<?> agregat = requete.source() == SourceAnalyse.LIGNES ? PilotageVenteLigneJour.class : PilotageVenteJour.class;
        Root<?> racine = query.from(agregat);
        Dimensions dimensions = new Dimensions(cb, racine);

        List<Selection<?>> selections = new ArrayList<>();
        List<Expression<?>> regroupements = new ArrayList<>();
        for (int rang = 0; rang < requete.axes().size(); rang++) {
            Dimension dimension = dimensions.lire(requete.axes().get(rang));
            selections.add(dimension.cle().alias("cle" + rang));
            selections.add(cb.greatest(dimension.libelle()).alias("libelle" + rang));
            regroupements.add(dimension.cle());
        }
        if (requete.parJour()) {
            selections.add(racine.get("jour").alias("jour"));
            regroupements.add(racine.get("jour"));
        }
        selections.addAll(selectionnerMesures(cb, racine, requete.source()));

        query.select(cb.tuple(selections)).where(filtrer(cb, racine, dimensions, requete)).groupBy(regroupements);
        return em.createQuery(query).getResultList().stream().map(tuple -> lire(tuple, requete)).toList();
    }

    private static List<Selection<?>> selectionnerMesures(CriteriaBuilder cb, Root<?> racine, SourceAnalyse source) {
        List<Selection<?>> mesures = new ArrayList<>(List.of(
            cb.sum(racine.<Long>get("montantTtc")).alias("caTtc"),
            cb.sum(racine.<Long>get("montantHt")).alias("caHt"),
            cb.sum(racine.<Long>get("remise")).alias("remise")
        ));
        if (source == SourceAnalyse.LIGNES) {
            mesures.add(cb.sum(racine.<Long>get("coutHt")).alias("coutHt"));
            mesures.add(cb.sum(racine.<Long>get("quantiteServie")).alias("quantite"));
        } else {
            mesures.add(cb.sum(racine.<Long>get("nbVentes")).alias("nbVentes"));
            mesures.add(cb.sum(racine.<Long>get("partTiersPayant")).alias("partTiersPayant"));
        }
        return mesures;
    }

    private static Predicate[] filtrer(CriteriaBuilder cb, Root<?> racine, Dimensions dimensions, RequeteVentilationDTO requete) {
        List<Predicate> conditions = new ArrayList<>();
        conditions.add(cb.between(racine.get("jour"), requete.du(), requete.au()));
        conditions.add(racine.get("categorieChiffreAffaire").in(requete.categories()));
        if (requete.source() == SourceAnalyse.ENTETES) {
            conditions.add(cb.isFalse(racine.get("annulee")));
        }
        for (FiltreAnalyseDTO filtre : requete.filtres()) {
            conditions.add(
                filtre.axe() == AxeAnalyse.REMISE
                    ? retenirTranches(cb, racine.get("tauxRemise"), filtre.cles())
                    : retenir(cb, dimensions.lire(filtre.axe()).cle(), filtre.cles())
            );
        }
        return conditions.toArray(Predicate[]::new);
    }

    /** Clé vide : l'élément « sans » (clé nulle en base, faute de jointure). */
    private static Predicate retenir(CriteriaBuilder cb, Expression<String> cle, List<String> cles) {
        List<String> renseignees = cles.stream().filter(c -> !c.isEmpty()).toList();
        Predicate parmi = renseignees.isEmpty() ? cb.disjunction() : cle.in(renseignees);
        return renseignees.size() == cles.size() ? parmi : cb.or(parmi, cle.isNull());
    }

    /** Une tranche s'écrit {@code min-max} (taux inclus). */
    private static Predicate retenirTranches(CriteriaBuilder cb, Expression<Short> taux, List<String> tranches) {
        return cb.or(
            tranches
                .stream()
                .map(tranche -> tranche.split("-"))
                .map(bornes -> cb.between(taux, Short.valueOf(bornes[0]), Short.valueOf(bornes[bornes.length - 1])))
                .toArray(Predicate[]::new)
        );
    }

    private static MesuresVentileesDTO lire(Tuple tuple, RequeteVentilationDTO requete) {
        int nombreAxes = requete.axes().size();
        boolean lignes = requete.source() == SourceAnalyse.LIGNES;
        return new MesuresVentileesDTO(
            nombreAxes > 0 ? tuple.get("cle0", String.class) : null,
            nombreAxes > 0 ? tuple.get("libelle0", String.class) : null,
            nombreAxes > 1 ? tuple.get("cle1", String.class) : null,
            nombreAxes > 1 ? tuple.get("libelle1", String.class) : null,
            requete.parJour() ? tuple.get("jour", LocalDate.class) : null,
            lignes ? 0 : somme(tuple, "nbVentes"),
            somme(tuple, "caTtc"),
            somme(tuple, "caHt"),
            somme(tuple, "remise"),
            lignes ? somme(tuple, "coutHt") : 0,
            lignes ? somme(tuple, "quantite") : 0,
            lignes ? 0 : somme(tuple, "partTiersPayant")
        );
    }

    private static long somme(Tuple tuple, String alias) {
        Number valeur = tuple.get(alias, Number.class);
        return valeur == null ? 0 : valeur.longValue();
    }

    private record Dimension(Expression<String> cle, Expression<String> libelle) {}

    /** Expressions des axes d'une requête, construites une fois chacune : un axe et son filtre partagent la même jointure. */
    private static final class Dimensions {

        private final CriteriaBuilder cb;
        private final Root<?> racine;
        private final Map<AxeAnalyse, Dimension> parAxe = new EnumMap<>(AxeAnalyse.class);
        private Join<?, Produit> produit;

        Dimensions(CriteriaBuilder cb, Root<?> racine) {
            this.cb = cb;
            this.racine = racine;
        }

        Dimension lire(AxeAnalyse axe) {
            return parAxe.computeIfAbsent(axe, this::construire);
        }

        private Dimension construire(AxeAnalyse axe) {
            return switch (axe) {
                case PRODUIT -> referentiel(joindreProduit());
                case FAMILLE -> referentiel(joindreProduit().join("famille", JoinType.LEFT));
                case LABORATOIRE -> referentiel(joindreProduit().join("laboratoire", JoinType.LEFT));
                case FORME -> referentiel(joindreProduit().join("forme", JoinType.LEFT));
                case GAMME -> referentiel(joindreProduit().join("gamme", JoinType.LEFT));
                case DCI -> referentiel(joindreProduit().join("dci", JoinType.LEFT));
                case FOURNISSEUR -> referentiel(joindreProduit().join("fournisseurProduitPrincipal", JoinType.LEFT).join("fournisseur", JoinType.LEFT));
                case TVA -> {
                    Join<?, ?> tva = joindreProduit().join("tva", JoinType.LEFT);
                    yield new Dimension(texte(tva.get("id")), texte(tva.get("taux")));
                }
                case NATURE_VENTE -> code(racine.get("natureVente"));
                case TYPE_PRESCRIPTION -> code(racine.get("typePrescription"));
                case HEURE -> code(racine.get("heure"));
                case REMISE -> code(racine.get("tauxRemise"));
                case OCTROI_REMISE -> code(racine.get("octroiRemise"));
                case VENDEUR -> utilisateur("vendeurId");
                case CAISSIER -> utilisateur("caissierId");
                case JOUR_SEMAINE, PERIODE -> throw new IllegalArgumentException(axe + " se déduit du jour, il n'est pas lu en base");
            };
        }

        private Join<?, Produit> joindreProduit() {
            if (produit == null) {
                produit = racine.join(Produit.class, JoinType.LEFT);
                produit.on(cb.equal(produit.get("id"), racine.get("produitId")));
            }
            return produit;
        }

        private Dimension utilisateur(String attribut) {
            Join<?, AppUser> utilisateur = racine.join(AppUser.class, JoinType.LEFT);
            utilisateur.on(cb.equal(utilisateur.get("id"), racine.get(attribut)));
            return new Dimension(texte(utilisateur.get("id")), cb.concat(cb.concat(utilisateur.get("firstName"), " "), utilisateur.get("lastName")));
        }

        private Dimension referentiel(From<?, ?> table) {
            return new Dimension(texte(table.get("id")), table.get("libelle"));
        }

        private Dimension code(Expression<?> colonne) {
            Expression<String> code = texte(colonne);
            return new Dimension(code, code);
        }

        private static Expression<String> texte(Expression<?> expression) {
            return expression.cast(String.class);
        }
    }
}
