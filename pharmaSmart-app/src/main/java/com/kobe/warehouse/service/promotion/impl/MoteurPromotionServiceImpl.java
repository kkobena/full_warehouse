package com.kobe.warehouse.service.promotion.impl;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.PromotionProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.repository.PromotionRepository;
import com.kobe.warehouse.service.promotion.CalculPromotion;
import com.kobe.warehouse.service.promotion.MoteurPromotionService;
import com.kobe.warehouse.service.promotion.OffreNM;
import com.kobe.warehouse.service.promotion.Pack;
import com.kobe.warehouse.service.promotion.RemiseRepartie;
import com.kobe.warehouse.service.promotion.SeuilMontant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MoteurPromotionServiceImpl implements MoteurPromotionService {

    private final PromotionRepository promotionRepository;

    public MoteurPromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public void effacer(Sales vente) {
        for (SalesLine ligne : vente.getSalesLines()) {
            if (ligne.getPromotionId() != null) {
                ligne.setDiscountAmount(0);
                ligne.setTauxRemise(0f);
                ligne.setPromotionId(null);
                ligne.setPromotionLibelle(null);
                ligne.setQuantitePromo(null);
                ligne.setRemisePromotion(0);
            }
        }
    }

    @Override
    public boolean appliquer(Sales vente) {
        if (!estEligible(vente)) {
            return false;
        }
        // Le magasin et la date de la vente, pas ceux de l'utilisateur ni d'aujourd'hui : une vente
        // reprise le lendemain, ou ouverte par délégation, garde ses prix.
        LocalDate date = Objects.requireNonNullElse(vente.getSaleDate(), LocalDate.now());
        List<Promotion> promotions =
                promotionRepository.findEnVigueur(vente.getMagasin().getId(), date, StatutPromotion.VALIDEE);
        if (promotions.isEmpty()) {
            return false;
        }
        List<Promotion> ordonnees = ordonner(promotions);
        Map<Integer, List<Candidate>> parProduit = indexerParProduit(ordonnees);
        Plafonds plafonds = new Plafonds(promotions, consommees(promotions));
        List<SalesLine> lignes = lignesOrdonnees(vente);
        Set<SalesLine> prises = Collections.newSetFromMap(new IdentityHashMap<>());

        // 1. Les offres N+M, les packs et les remises de panier groupent plusieurs lignes : ils passent d'abord, dans
        // l'ordre
        // de départage. Une ligne prise par l'un n'appartient plus qu'à lui.
        for (Promotion groupe : ordonnees) {
            if (estDeGroupe(groupe)) {
                appliquerGroupe(groupe, lignes, prises, parProduit, plafonds);
            }
        }

        // 2. Les promotions d'une ligne, sur les lignes qu'aucune offre n'a prises. Une ligne remisée par
        // le caissier ne reçoit qu'une promotion cumulable, après sa remise (invariant 7).
        for (SalesLine ligne : lignes) {
            if (prises.contains(ligne) || ligne.getProduit() == null) {
                continue;
            }
            Choix choix = meilleureParLigne(ligne, parProduit, plafonds);
            if (choix != null) {
                Promotion promotion = choix.promotion();
                int unites =
                        CalculPromotion.unitesRemisees(promotion, quantite(ligne), plafonds.disponibles(promotion));
                poser(ligne, promotion, remiseCaissier(ligne) + choix.remise(), choix.remise(), unites);
                plafonds.imputer(promotion, unites);
                prises.add(ligne);
            }
        }
        return !prises.isEmpty();
    }

    private static boolean estDeGroupe(Promotion promotion) {
        return promotion.getType() == TypePromotion.PROMO_OFFRE_NM
                || promotion.getType() == TypePromotion.PROMO_PACK
                || promotion.getType() == TypePromotion.PROMO_SEUIL_MONTANT;
    }

    /**
     * Une offre N+M ou un pack sur tout le panier : un groupe mêle plusieurs lignes. Il ne les prend
     * que s'il fait gagner plus à la cliente que les promotions d'une ligne sur ces mêmes lignes — le
     * moteur retient toujours la plus avantageuse (plan §5.4).
     */
    private static void appliquerGroupe(
            Promotion offre,
            List<SalesLine> lignes,
            Set<SalesLine> prises,
            Map<Integer, List<Candidate>> parProduit,
            Plafonds plafonds) {
        Set<Integer> perimetre = new HashSet<>();
        offre.getProduits()
                .forEach(article -> perimetre.add(article.getProduit().getId()));
        List<SalesLine> concernees = lignes.stream()
                .filter(l -> !prises.contains(l)
                        && estLibre(l)
                        && perimetre.contains(l.getProduit().getId()))
                .toList();
        if (concernees.isEmpty()) {
            return;
        }
        RemiseRepartie resultat = switch (offre.getType()) {
            case PROMO_SEUIL_MONTANT ->
                SeuilMontant.appliquer(
                        Objects.requireNonNullElse(offre.getSeuilMontant(), 0),
                        Objects.requireNonNullElse(offre.getValeur(), 0),
                        concernees.stream()
                                .map(l -> new SeuilMontant.Ligne(
                                        Objects.requireNonNullElse(l.getRegularUnitPrice(), 0), quantite(l)))
                                .toList());
            case PROMO_PACK ->
                Pack.appliquer(
                        composants(offre),
                        Objects.requireNonNullElse(offre.getValeur(), 0),
                        concernees.stream()
                                .map(l -> new Pack.Ligne(
                                        l.getProduit().getId(),
                                        Objects.requireNonNullElse(l.getRegularUnitPrice(), 0),
                                        quantite(l)))
                                .toList(),
                        plafonds.disponibles(offre));
            default ->
                OffreNM.appliquer(
                        offre.getQuantiteMin(),
                        Objects.requireNonNullElse(offre.getValeur(), 0),
                        concernees.stream()
                                .map(l -> new OffreNM.Ligne(
                                        Objects.requireNonNullElse(l.getRegularUnitPrice(), 0), quantite(l)))
                                .toList(),
                        plafonds.disponibles(offre));
        };
        if (resultat.remiseTotale() == 0) {
            return;
        }
        long alternative = 0;
        for (int i = 0; i < concernees.size(); i++) {
            if (resultat.unites()[i] > 0) {
                Choix choix = meilleureParLigne(concernees.get(i), parProduit, plafonds);
                alternative += choix == null ? 0 : choix.remise();
            }
        }
        // Strictement : à remise égale, la promotion d'une ligne, plus simple à lire sur le ticket.
        if (resultat.remiseTotale() <= alternative) {
            return;
        }
        for (int i = 0; i < concernees.size(); i++) {
            if (resultat.unites()[i] > 0) {
                // Une ligne d'un groupe n'a pas de remise du caissier (estLibre) : toute sa remise est
                // celle de la promotion.
                poser(concernees.get(i), offre, resultat.remises()[i], resultat.remises()[i], resultat.unites()[i]);
                prises.add(concernees.get(i));
            }
        }
        plafonds.imputer(offre, resultat.unitesTotales());
    }

    /** Les articles d'un pack et leur quantité, dans l'ordre de leur identifiant : rien ne dépend d'un ensemble. */
    private static Map<Integer, Integer> composants(Promotion pack) {
        Map<Integer, Integer> composants = new TreeMap<>();
        pack.getProduits().forEach(pp -> composants.merge(pp.getProduit().getId(), pp.getQuantite(), Integer::sum));
        return composants;
    }

    /** La promotion d'une ligne la plus avantageuse ; nulle si aucune ne remise. Offres et packs n'y concourent pas. */
    private static Choix meilleureParLigne(
            SalesLine ligne, Map<Integer, List<Candidate>> parProduit, Plafonds plafonds) {
        List<Candidate> candidates = parProduit.get(ligne.getProduit().getId());
        if (candidates == null) {
            return null;
        }
        int prix = Objects.requireNonNullElse(ligne.getRegularUnitPrice(), 0);
        int caissier = remiseCaissier(ligne);
        Choix meilleur = null;
        for (Candidate candidate : candidates) {
            if (estDeGroupe(candidate.promotion())
                    || (caissier > 0 && !candidate.promotion().isCumulable())) {
                continue;
            }
            int remise = caissier > 0
                    ? CalculPromotion.remiseCumulee(
                            candidate.promotion(),
                            candidate.article(),
                            prix,
                            quantite(ligne),
                            caissier,
                            PromotionServiceImpl.prixAchat(ligne.getProduit()),
                            plafonds.disponibles(candidate.promotion()))
                    : CalculPromotion.remiseLigne(
                            candidate.promotion(),
                            candidate.article(),
                            prix,
                            quantite(ligne),
                            plafonds.disponibles(candidate.promotion()));
            // Strictement supérieure : à remise égale, la première dans l'ordre de départage garde la ligne.
            if (remise > 0 && (meilleur == null || remise > meilleur.remise())) {
                meilleur = new Choix(candidate.promotion(), remise);
            }
        }
        return meilleur;
    }

    /** Sans remise du caissier : seule une telle ligne entre dans une offre, un pack ou une remise de panier. */
    private static boolean estLibre(SalesLine ligne) {
        return ligne.getProduit() != null && remiseCaissier(ligne) == 0;
    }

    /**
     * La remise posée par le caissier (remise produit de la vente), lue après {@link #effacer} : ce
     * qui reste sur la ligne n'est pas du moteur. Jamais écrasée ; une promotion cumulable s'y ajoute.
     */
    private static int remiseCaissier(SalesLine ligne) {
        return ligne.getPromotionId() != null ? 0 : Objects.requireNonNullElse(ligne.getDiscountAmount(), 0);
    }

    private static int quantite(SalesLine ligne) {
        return Objects.requireNonNullElse(ligne.getQuantityRequested(), 0);
    }

    /**
     * Ventes au comptant et ventes à crédit (assurance, carnet) — pas une vente dépôt, qui est un
     * transfert.
     */
    private static boolean estEligible(Sales vente) {
        return (vente instanceof CashSale || vente instanceof ThirdPartySales)
                && vente.getMagasin() != null
                && !vente.getSalesLines().isEmpty();
    }

    /**
     * L'ordre de départage : priorité décroissante, puis identifiant croissant. Jamais l'ordre d'un
     * ensemble, qui changerait d'une lecture à l'autre.
     */
    private static List<Promotion> ordonner(List<Promotion> promotions) {
        List<Promotion> ordonnees = new ArrayList<>(promotions);
        ordonnees.sort(
                Comparator.comparingInt(Promotion::getPriorite).reversed().thenComparing(Promotion::getId));
        return ordonnees;
    }

    /** Les candidates de chaque produit, dans l'ordre des promotions reçues. */
    private static Map<Integer, List<Candidate>> indexerParProduit(List<Promotion> ordonnees) {
        Map<Integer, List<Candidate>> parProduit = new HashMap<>();
        for (Promotion promotion : ordonnees) {
            for (PromotionProduit article : promotion.getProduits()) {
                parProduit
                        .computeIfAbsent(article.getProduit().getId(), id -> new ArrayList<>())
                        .add(new Candidate(promotion, article));
            }
        }
        return parProduit;
    }

    /** Le compteur du plafond total, lu seulement pour les promotions qui en ont un. */
    private Map<Long, Long> consommees(List<Promotion> promotions) {
        List<Long> ids = promotions.stream()
                .filter(p -> p.getQuantiteMaxTotale() != null)
                .map(Promotion::getId)
                .toList();
        Map<Long, Long> unites = new HashMap<>();
        if (!ids.isEmpty()) {
            promotionRepository.sommerUnitesRemisees(ids).forEach(u -> unites.put(u.getPromotionId(), u.getUnites()));
        }
        return unites;
    }

    /**
     * Un plafond partagé se répartit dans l'ordre des lignes : celui de leur saisie, jamais celui d'un
     * ensemble, sans quoi la ligne servie changerait d'un recalcul à l'autre.
     */
    /**
     * Une ligne qu'une promotion peut remiser. Sur une vente assurance, seules les lignes non
     * remboursées : le patient les paie en entier, hors de la prise en charge ; remiser une ligne
     * couverte déplacerait la charge entre l'assureur et le patient (plan §8, question 2). Une vente
     * carnet se remise sur toutes ses lignes, comme la remise produit.
     */
    private static boolean estPromouvable(Sales vente, SalesLine ligne) {
        return !(vente instanceof ThirdPartySales tiersPayant)
                || tiersPayant.getNatureVente() != NatureVente.ASSURANCE
                || ligne.isNonRembourse();
    }

    private static List<SalesLine> lignesOrdonnees(Sales vente) {
        List<SalesLine> lignes = new ArrayList<>(vente.getSalesLines().stream()
                .filter(l -> estPromouvable(vente, l))
                .toList());
        lignes.sort(Comparator.comparing(
                        (SalesLine l) -> l.getId() == null ? null : l.getId().getId(),
                        Comparator.nullsLast(Comparator.<Long>naturalOrder()))
                .thenComparing(
                        l -> l.getProduit() == null ? null : l.getProduit().getId(),
                        Comparator.nullsLast(Comparator.<Integer>naturalOrder())));
        return lignes;
    }

    /**
     * @param remise la remise totale de la ligne, remise du caissier comprise sur une ligne cumulée
     * @param partPromotion ce que la promotion y ajoute : le journal des remises sépare les deux
     */
    private static void poser(SalesLine ligne, Promotion promotion, int remise, int partPromotion, int unites) {
        ligne.setDiscountAmount(remise);
        ligne.setRemisePromotion(partPromotion);
        int montant = Objects.requireNonNullElse(ligne.getSalesAmount(), 0);
        // La FNE déclare la remise par le taux, en fraction comme la grille : il se calcule aussi
        // pour un montant ou un prix fixe.
        ligne.setTauxRemise(montant > 0 ? (float) remise / montant : 0f);
        ligne.setPromotionId(promotion.getId());
        ligne.setPromotionLibelle(promotion.getLibelle());
        ligne.setQuantitePromo(unites);
    }

    /**
     * Ce que les plafonds laissent encore à la vente, promotion par promotion. Le plafond par vente
     * vaut pour le ticket, pas pour chaque ligne : deux articles d'une même promotion le partagent. Le
     * plafond total est diminué des unités des ventes clôturées, puis de celles du panier.
     */
    private static final class Plafonds {

        private final Map<Long, Integer> restants = new HashMap<>();

        Plafonds(List<Promotion> promotions, Map<Long, Long> consommees) {
            for (Promotion promotion : promotions) {
                Integer restant = promotion.getQuantiteMaxParVente();
                if (promotion.getQuantiteMaxTotale() != null) {
                    long reste = promotion.getQuantiteMaxTotale() - consommees.getOrDefault(promotion.getId(), 0L);
                    int total = (int) Math.max(0, reste);
                    restant = restant == null ? total : Math.min(restant, total);
                }
                if (restant != null) {
                    restants.put(promotion.getId(), restant);
                }
            }
        }

        /** Nul : aucune borne. */
        Integer disponibles(Promotion promotion) {
            return restants.get(promotion.getId());
        }

        void imputer(Promotion promotion, int unites) {
            restants.computeIfPresent(promotion.getId(), (id, restant) -> Math.max(0, restant - unites));
        }
    }

    private record Candidate(Promotion promotion, PromotionProduit article) {}

    private record Choix(Promotion promotion, int remise) {}
}
