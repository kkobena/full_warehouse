package com.kobe.warehouse.service.promotion.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.PromotionProduit;
import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.PromotionRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.promotion.ChoixPerimetreDTO;
import com.kobe.warehouse.service.dto.promotion.EtatPromotion;
import com.kobe.warehouse.service.dto.promotion.PackComptoirDTO;
import com.kobe.warehouse.service.dto.promotion.PerimetreDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionArticleDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionFormDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionProduitDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionProduitFormDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.portee.PorteeMagasinService;
import com.kobe.warehouse.service.promotion.CalculPromotion;
import com.kobe.warehouse.service.promotion.EtiquettesPromotion;
import com.kobe.warehouse.service.promotion.PromotionService;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {

    static final String ENTITY_NAME = "promotion";

    /** Au-delà, le message d'erreur cite « et N autres » plutôt que de dérouler la liste. */
    private static final int ARTICLES_CITES = 5;

    private final PromotionRepository promotionRepository;
    private final ProduitRepository produitRepository;
    private final PorteeMagasinService porteeMagasinService;
    private final UserService userService;

    public PromotionServiceImpl(
            PromotionRepository promotionRepository,
            ProduitRepository produitRepository,
            PorteeMagasinService porteeMagasinService,
            UserService userService) {
        this.promotionRepository = promotionRepository;
        this.produitRepository = produitRepository;
        this.porteeMagasinService = porteeMagasinService;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDTO> lister() {
        Integer magasinId = magasin().getId();
        LocalDate aujourdhui = LocalDate.now();
        List<Promotion> promotions = promotionRepository.findAllByMagasinId(magasinId);
        Map<Long, Long> unites = unitesRemisees(promotions);
        return promotions.stream()
                .map(p -> toDto(p, aujourdhui, false, unites.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDTO lire(Long id) {
        return toDto(charger(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionArticleDTO> articlesEnVigueur() {
        LocalDate aujourdhui = LocalDate.now();
        List<Promotion> promotions = new ArrayList<>(
                promotionRepository.findEnVigueur(magasin().getId(), aujourdhui, StatutPromotion.VALIDEE));
        Map<Long, Long> unites = unitesRemisees(promotions);
        promotions.removeIf(
                p -> EtatPromotion.de(p, aujourdhui, unites.getOrDefault(p.getId(), 0L)) == EtatPromotion.EPUISEE);
        // L'ordre de départage du moteur : à prix égal, la première garde l'article.
        promotions.sort(
                Comparator.comparingInt(Promotion::getPriorite).reversed().thenComparing(Promotion::getId));
        Map<Integer, PromotionArticleDTO> parProduit = new HashMap<>();
        for (Promotion promotion : promotions) {
            for (PromotionProduit article : promotion.getProduits()) {
                Produit produit = article.getProduit();
                int prix = Objects.requireNonNullElse(produit.getRegularUnitPrice(), 0);
                int apres = CalculPromotion.prixUnitaireApres(promotion, article, prix);
                if (apres >= prix) {
                    continue;
                }
                PromotionArticleDTO retenu = parProduit.get(produit.getId());
                if (retenu == null || apres < retenu.prixApresPromo()) {
                    parProduit.put(
                            produit.getId(),
                            new PromotionArticleDTO(
                                    produit.getId(),
                                    promotion.getId(),
                                    promotion.getLibelle(),
                                    prix,
                                    apres,
                                    promotion.getDateFin(),
                                    CalculPromotion.seuil(promotion),
                                    offre(promotion)));
                }
            }
        }
        return List.copyOf(parProduit.values());
    }

    @Override
    public PromotionDTO creer(PromotionFormDTO form) {
        Promotion promotion = new Promotion().setCreatedBy(userService.getUser());
        promotion.getMagasins().add(magasin());
        remplir(promotion, form);
        controler(promotion);
        promotion = promotionRepository.save(promotion);
        promotion.setCode(String.format("PROMO-%05d", promotion.getId()));
        attribuerCodeBarrePack(promotion);
        return toDto(promotion, LocalDate.now(), true, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public PackComptoirDTO pack(String codeBarre) {
        LocalDate aujourdhui = LocalDate.now();
        String code = codeBarre == null ? "" : codeBarre.trim();
        Promotion pack =
                promotionRepository.findEnVigueur(magasin().getId(), aujourdhui, StatutPromotion.VALIDEE).stream()
                        .filter(p ->
                                p.getType() == TypePromotion.PROMO_PACK && code.equalsIgnoreCase(p.getCodeBarrePack()))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestAlertException(
                                "Aucun pack en vigueur pour le code " + code + ".", ENTITY_NAME, "packinconnu"));
        long unites = unitesRemisees(List.of(pack)).getOrDefault(pack.getId(), 0L);
        if (EtatPromotion.de(pack, aujourdhui, unites) == EtatPromotion.EPUISEE) {
            throw new BadRequestAlertException(
                    "Le pack « " + pack.getLibelle() + " » est épuisé.", ENTITY_NAME, "packepuise");
        }
        List<PackComptoirDTO.Composant> composants = pack.getProduits().stream()
                .sorted(Comparator.comparing(pp -> pp.getProduit().getId()))
                .map(pp -> {
                    Produit produit = pp.getProduit();
                    return new PackComptoirDTO.Composant(
                            produit.getId(),
                            produit.getLibelle(),
                            Objects.requireNonNullElse(produit.getRegularUnitPrice(), 0),
                            pp.getQuantite(),
                            Objects.requireNonNullElse(produit.getItemQty(), 1),
                            produit.getParent() == null
                                    ? null
                                    : produit.getParent().getId());
                })
                .toList();
        return new PackComptoirDTO(
                pack.getId(), pack.getLibelle(), Objects.requireNonNullElse(pack.getValeur(), 0), composants);
    }

    @Override
    @Transactional(readOnly = true)
    public ChoixPerimetreDTO choixPerimetre() {
        List<PromotionRepository.OptionPerimetre> rayons =
                promotionRepository.rayonsDuMagasin(magasin().getId());
        // Le nom du stockage n'est cité que s'il départage deux rayons homonymes.
        Map<String, Long> homonymes = new HashMap<>();
        rayons.forEach(r -> homonymes.merge(r.getLibelle(), 1L, Long::sum));
        return new ChoixPerimetreDTO(
                rayons.stream()
                        .map(r -> new ChoixPerimetreDTO.Option(
                                r.getId(),
                                homonymes.get(r.getLibelle()) > 1
                                        ? r.getLibelle() + " · " + r.getStockage()
                                        : r.getLibelle(),
                                r.getArticles()))
                        .toList(),
                promotionRepository.famillesActives().stream()
                        .map(f -> new ChoixPerimetreDTO.Option(f.getId(), f.getLibelle(), f.getArticles()))
                        .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PerimetreDTO perimetre(Integer rayonId, Integer familleId) {
        if ((rayonId == null) == (familleId == null)) {
            throw erreur("Choisissez un rayon ou une famille.", "perimetre");
        }
        List<Produit> produits = rayonId != null
                ? promotionRepository.produitsDuRayon(rayonId, magasin().getId())
                : promotionRepository.produitsDeLaFamille(familleId);
        List<PerimetreDTO.Article> articles = new ArrayList<>();
        List<String> administres = new ArrayList<>();
        produits.stream()
                .sorted(Comparator.comparing(Produit::getLibelle, Comparator.nullsLast(String::compareTo)))
                .forEach(p -> {
                    // Ajouté comme les autres : c'est au pharmacien de décider s'il le garde.
                    boolean administre = estPrixAdministre(p);
                    if (administre) {
                        administres.add(p.getLibelle());
                    }
                    FournisseurProduit principal = p.getFournisseurProduitPrincipal();
                    articles.add(new PerimetreDTO.Article(
                            p.getId(),
                            p.getLibelle(),
                            principal != null ? principal.getCodeCip() : null,
                            Objects.requireNonNullElse(p.getRegularUnitPrice(), 0),
                            prixAchat(p),
                            administre));
                });
        return new PerimetreDTO(articles, administres);
    }

    static boolean estPrixAdministre(Produit produit) {
        return produit.getTva() == null
                || Objects.requireNonNullElse(produit.getTva().getTaux(), 0) == 0;
    }

    /** « PK » + 8 chiffres : lisible en Code 39, et jamais confondu avec un code d'article. */
    private static void attribuerCodeBarrePack(Promotion promotion) {
        if (promotion.getType() == TypePromotion.PROMO_PACK && promotion.getCodeBarrePack() == null) {
            promotion.setCodeBarrePack(String.format("PK%08d", promotion.getId()));
        }
    }

    /** Ce que le badge dit d'une offre ou d'un pack, qui n'ont pas de prix unitaire remisé. */
    private static String offre(Promotion promotion) {
        if (promotion.getType() == TypePromotion.PROMO_PACK) {
            return "en pack à " + NumberUtil.formatToString(Objects.requireNonNullElse(promotion.getValeur(), 0))
                    + " CFA";
        }
        return EtiquettesPromotion.offre(
                promotion.getType(), promotion.getQuantiteMin(), promotion.getValeur(), promotion.getSeuilMontant());
    }

    @Override
    public PromotionDTO modifier(Long id, PromotionFormDTO form) {
        Promotion promotion = charger(id);
        if (promotion.getStatut() != StatutPromotion.BROUILLON && promotion.getStatut() != StatutPromotion.SUSPENDUE) {
            throw new BadRequestAlertException(
                    "Seule une promotion en brouillon ou suspendue se modifie : suspendez-la d'abord.",
                    ENTITY_NAME,
                    "nonmodifiable");
        }
        remplir(promotion, form);
        controler(promotion);
        attribuerCodeBarrePack(promotion);
        promotion.setUpdatedAt(LocalDateTime.now());
        return toDto(promotion);
    }

    @Override
    public PromotionDTO valider(Long id) {
        Promotion promotion = charger(id);
        if (promotion.getStatut() != StatutPromotion.BROUILLON && promotion.getStatut() != StatutPromotion.SUSPENDUE) {
            throw transitionRefusee(promotion, "valider");
        }
        // Les prix ont pu changer depuis la création : le contrôle se refait. Prix administré et vente à
        // perte ne bloquent pas : l'écran les a montrés, et a demandé confirmation avant d'appeler.
        controler(promotion);
        return changerStatut(promotion, StatutPromotion.VALIDEE);
    }

    @Override
    public PromotionDTO suspendre(Long id) {
        Promotion promotion = charger(id);
        if (promotion.getStatut() != StatutPromotion.VALIDEE) {
            throw transitionRefusee(promotion, "suspendre");
        }
        return changerStatut(promotion, StatutPromotion.SUSPENDUE);
    }

    @Override
    public PromotionDTO annuler(Long id) {
        Promotion promotion = charger(id);
        if (promotion.getStatut() == StatutPromotion.ANNULEE) {
            throw transitionRefusee(promotion, "annuler");
        }
        return changerStatut(promotion, StatutPromotion.ANNULEE);
    }

    private PromotionDTO changerStatut(Promotion promotion, StatutPromotion statut) {
        promotion.setStatut(statut).setUpdatedAt(LocalDateTime.now());
        return toDto(promotion);
    }

    private BadRequestAlertException transitionRefusee(Promotion promotion, String geste) {
        return new BadRequestAlertException(
                "Impossible de " + geste + " une promotion "
                        + promotion.getStatut().name().toLowerCase() + ".",
                ENTITY_NAME,
                "transitionrefusee");
    }

    /** Une promotion d'un autre magasin n'existe pas pour l'utilisateur : écrire ailleurs est impossible. */
    private Promotion charger(Long id) {
        Integer magasinId = magasin().getId();
        return promotionRepository
                .findOneWithProduits(id)
                .filter(p -> p.getMagasins().stream().anyMatch(m -> Objects.equals(m.getId(), magasinId)))
                .orElseThrow(() -> new BadRequestAlertException("Promotion introuvable", ENTITY_NAME, "idnotfound"));
    }

    private Magasin magasin() {
        return porteeMagasinService.resoudreLeMagasinDExploitation();
    }

    private void remplir(Promotion promotion, PromotionFormDTO form) {
        promotion
                .setLibelle(form.libelle().trim())
                .setType(form.type())
                .setValeur(form.type() == TypePromotion.PROMO_PRIX_FIXE ? null : form.valeur())
                .setDateDebut(form.dateDebut())
                .setDateFin(form.dateFin())
                .setPriorite(form.priorite())
                .setQuantiteMin(Math.max(1, form.quantiteMin()))
                .setQuantiteMaxParVente(form.quantiteMaxParVente())
                .setQuantiteMaxTotale(form.quantiteMaxTotale())
                .setSeuilMontant(form.type() == TypePromotion.PROMO_SEUIL_MONTANT ? form.seuilMontant() : null)
                .setCumulable(Boolean.TRUE.equals(form.cumulable()));
        if (form.type() == TypePromotion.PROMO_SEUIL_MONTANT) {
            // Le seuil est en francs, pas en unités : « à partir de N unités » n'a pas de sens ici.
            promotion.setQuantiteMin(1);
        }

        Set<Integer> vus = new HashSet<>();
        Set<PromotionProduit> produits = new LinkedHashSet<>();
        for (PromotionProduitFormDTO ligne : form.produits()) {
            if (!vus.add(ligne.produitId())) {
                continue;
            }
            Produit produit = produitRepository
                    .findById(ligne.produitId())
                    .orElseThrow(() -> new BadRequestAlertException(
                            "Produit introuvable : " + ligne.produitId(), ENTITY_NAME, "produitinconnu"));
            Integer prixPromo = form.type() == TypePromotion.PROMO_PRIX_FIXE ? ligne.prixPromo() : null;
            int quantite = form.type() == TypePromotion.PROMO_PACK
                    ? Math.max(1, Objects.requireNonNullElse(ligne.quantite(), 1))
                    : 1;
            produits.add(new PromotionProduit(produit, prixPromo, quantite));
        }
        promotion.getProduits().clear();
        promotion.getProduits().addAll(produits);
    }

    /** Les invariants du §5.3 du plan. */
    void controler(Promotion promotion) {
        if (promotion.getMagasins().isEmpty()) {
            throw erreur("Une promotion vise au moins un magasin.", "sansmagasin");
        }
        if (promotion.getProduits().isEmpty()) {
            throw erreur("Une promotion porte sur au moins un article.", "sansproduit");
        }
        if (promotion.getDateDebut().isAfter(promotion.getDateFin())) {
            throw erreur("La date de début doit précéder la date de fin.", "dates");
        }
        if (promotion.getDateFin().isBefore(LocalDate.now())) {
            throw erreur("La date de fin est déjà passée.", "echeancepassee");
        }
        // La valeur d'abord : le seuil d'une offre N+M se calcule sur elle.
        controlerValeur(promotion);
        if (promotion.getType() == TypePromotion.PROMO_SEUIL_MONTANT) {
            controlerSeuilMontant(promotion);
        }
        if (promotion.isCumulable() && !CalculPromotion.estDeLigne(promotion)) {
            throw erreur(
                    "Seule une promotion en pourcentage, en montant ou à prix fixe se cumule avec la remise du caissier :"
                            + " une offre, un pack ou une remise de panier répartissent leur remise sur les prix normaux.",
                    "cumulgroupe");
        }
        int seuil = CalculPromotion.seuil(promotion);
        String quoi = promotion.getType() == TypePromotion.PROMO_OFFRE_NM
                ? "un groupe de l'offre (" + seuil + " unités)"
                : "la quantité minimale";
        Integer max = promotion.getQuantiteMaxParVente();
        if (max != null && max < seuil) {
            throw erreur("Le plafond par vente ne peut être inférieur à " + quoi + ".", "plafond");
        }
        Integer total = promotion.getQuantiteMaxTotale();
        if (total != null && total < seuil) {
            throw erreur("Le plafond total ne peut être inférieur à " + quoi + ".", "plafondtotal");
        }
        if (promotion.getType() == TypePromotion.PROMO_PACK) {
            controlerPack(promotion);
        }
    }

    /**
     * Un pack réunit au moins deux articles différents et coûte moins que ses articles vendus seuls.
     * Vendu sous le prix d'achat de ses articles, il est signalé ({@link #alertes}), pas refusé.
     */
    private void controlerPack(Promotion pack) {
        if (pack.getProduits().size() < 2) {
            throw erreur("Un pack réunit au moins deux articles différents.", "packunarticle");
        }
        int prix = Objects.requireNonNullElse(pack.getValeur(), 0);
        long reference = CalculPromotion.prixReferencePack(pack);
        if (prix >= reference) {
            throw erreur(
                    "Le prix du pack (" + prix + " F) doit être inférieur à celui de ses articles vendus seuls ("
                            + reference + " F).",
                    "packsansremise");
        }
    }

    private static long prixAchatPack(Promotion pack) {
        return pack.getProduits().stream()
                .mapToLong(pp -> (long) pp.getQuantite() * prixAchat(pp.getProduit()))
                .sum();
    }

    private void controlerValeur(Promotion promotion) {
        Integer valeur = promotion.getValeur();
        switch (promotion.getType()) {
            case PROMO_POURCENTAGE, PROMO_SEUIL_MONTANT -> {
                if (valeur == null || valeur < 1 || valeur > 99) {
                    throw erreur("Le taux d'une promotion est compris entre 1 et 99 %.", "taux");
                }
            }
            case PROMO_MONTANT -> {
                if (valeur == null || valeur < 1) {
                    throw erreur("Le montant de la remise doit être positif.", "montant");
                }
            }
            case PROMO_PACK -> {
                if (valeur == null || valeur < 1) {
                    throw erreur("Le prix du pack doit être positif.", "prixpack");
                }
            }
            case PROMO_OFFRE_NM -> {
                if (valeur == null || valeur < 1) {
                    throw erreur("Une offre N+M offre au moins une unité.", "offertes");
                }
            }
            case PROMO_PRIX_FIXE -> {
                List<String> sansPrix = promotion.getProduits().stream()
                        .filter(pp -> pp.getPrixPromo() == null || pp.getPrixPromo() < 1)
                        .map(pp -> pp.getProduit().getLibelle())
                        .toList();
                if (!sansPrix.isEmpty()) {
                    throw erreur("Prix promotionnel manquant pour " + citer(sansPrix) + ".", "prixfixe");
                }
            }
        }
    }

    /**
     * Une remise de panier : un seuil positif, et pas de plafond en unités — ils bornent des unités, la
     * remise porte sur un montant. Le plancher de marge, lui, se contrôle article par article comme pour
     * un pourcentage : c'est le pire cas, celui d'une unité remisée de tout le taux.
     */
    private void controlerSeuilMontant(Promotion promotion) {
        if (promotion.getSeuilMontant() == null || promotion.getSeuilMontant() < 1) {
            throw erreur("Le montant d'achat qui déclenche la remise doit être positif.", "seuilmontant");
        }
        if (promotion.getQuantiteMaxParVente() != null || promotion.getQuantiteMaxTotale() != null) {
            throw erreur(
                    "Une remise dès un montant d'achat ne se plafonne pas en unités : retirez les plafonds.",
                    "seuilplafond");
        }
    }

    /**
     * Invariants 3 et 4, devenus des signalements (décisions de l'officine, plan §8) : un médicament à
     * prix administré, une unité vendue au prix d'achat ou en dessous — chez le fournisseur principal,
     * à défaut le prix de revient. Une fin de série ou une péremption proche justifie une vente à
     * perte ; c'est au pharmacien d'en décider, l'écran le lui montre et lui demande confirmation.
     */
    static List<String> alertes(Promotion promotion) {
        List<String> alertes = new ArrayList<>();
        List<String> administres = promotion.getProduits().stream()
                .map(PromotionProduit::getProduit)
                .filter(PromotionServiceImpl::estPrixAdministre)
                .map(Produit::getLibelle)
                .sorted(Comparator.nullsLast(String::compareTo))
                .toList();
        if (!administres.isEmpty()) {
            alertes.add("Médicament à prix administré (TVA 0) : " + citer(administres) + ".");
        }
        if (promotion.getType() == TypePromotion.PROMO_PACK) {
            int prix = Objects.requireNonNullElse(promotion.getValeur(), 0);
            long achat = prixAchatPack(promotion);
            if (!promotion.getProduits().isEmpty() && prix <= achat) {
                alertes.add("Pack vendu à perte : " + prix + " F pour un achat de " + achat + " F.");
            }
            return alertes;
        }
        List<String> aPerte = new ArrayList<>();
        for (PromotionProduit article : promotion.getProduits()) {
            if (estAPerte(promotion, article)) {
                Produit produit = article.getProduit();
                int apres = CalculPromotion.prixUnitaireApres(
                        promotion, article, Objects.requireNonNullElse(produit.getRegularUnitPrice(), 0));
                aPerte.add(produit.getLibelle() + " (" + apres + " F pour un achat à " + prixAchat(produit) + " F)");
            }
        }
        if (!aPerte.isEmpty()) {
            aPerte.sort(Comparator.naturalOrder());
            alertes.add("Vendu à perte : " + citer(aPerte) + ".");
        }
        return alertes;
    }

    /** Une unité vendue en promotion au prix d'achat ou en dessous ; un pack se juge en entier. */
    private static boolean estAPerte(Promotion promotion, PromotionProduit article) {
        if (promotion.getType() == TypePromotion.PROMO_PACK) {
            return false;
        }
        Produit produit = article.getProduit();
        int prix = Objects.requireNonNullElse(produit.getRegularUnitPrice(), 0);
        return CalculPromotion.prixUnitaireApres(promotion, article, prix) <= prixAchat(produit);
    }

    static int prixAchat(Produit produit) {
        FournisseurProduit principal = produit.getFournisseurProduitPrincipal();
        if (principal != null && principal.getPrixAchat() != null && principal.getPrixAchat() > 0) {
            return principal.getPrixAchat();
        }
        return Objects.requireNonNullElse(produit.getCostAmount(), 0);
    }

    private static String citer(List<String> elements) {
        if (elements.size() <= ARTICLES_CITES) {
            return String.join(", ", elements);
        }
        return String.join(", ", elements.subList(0, ARTICLES_CITES)) + " et " + (elements.size() - ARTICLES_CITES)
                + " autres";
    }

    private static BadRequestAlertException erreur(String message, String cle) {
        return new BadRequestAlertException(message, ENTITY_NAME, cle);
    }

    private PromotionDTO toDto(Promotion promotion) {
        return toDto(
                promotion,
                LocalDate.now(),
                true,
                unitesRemisees(List.of(promotion)).getOrDefault(promotion.getId(), 0L));
    }

    /** Le compteur du plafond total, lu seulement pour les promotions qui en ont un. */
    private Map<Long, Long> unitesRemisees(List<Promotion> promotions) {
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

    private static PromotionDTO toDto(
            Promotion promotion, LocalDate aujourdhui, boolean avecProduits, long unitesRemisees) {
        List<PromotionProduitDTO> produits = List.of();
        if (avecProduits) {
            Map<Integer, PromotionProduitDTO> parId = new HashMap<>();
            for (PromotionProduit article : promotion.getProduits()) {
                Produit produit = article.getProduit();
                int prix = Objects.requireNonNullElse(produit.getRegularUnitPrice(), 0);
                FournisseurProduit principal = produit.getFournisseurProduitPrincipal();
                parId.put(
                        produit.getId(),
                        new PromotionProduitDTO(
                                produit.getId(),
                                produit.getLibelle(),
                                principal == null ? null : principal.getCodeCip(),
                                prix,
                                article.getPrixPromo(),
                                CalculPromotion.prixUnitaireApres(promotion, article, prix),
                                article.getQuantite(),
                                estPrixAdministre(produit),
                                estAPerte(promotion, article)));
            }
            produits = parId.values().stream()
                    .sorted(Comparator.comparing(PromotionProduitDTO::libelle, Comparator.nullsLast(String::compareTo)))
                    .toList();
        }
        return new PromotionDTO(
                promotion.getId(),
                promotion.getCode(),
                promotion.getLibelle(),
                promotion.getType(),
                promotion.getValeur(),
                promotion.getDateDebut(),
                promotion.getDateFin(),
                promotion.getStatut(),
                EtatPromotion.de(promotion, aujourdhui, unitesRemisees),
                promotion.getPriorite(),
                promotion.getQuantiteMin(),
                promotion.getQuantiteMaxParVente(),
                promotion.getQuantiteMaxTotale(),
                unitesRemisees,
                promotion.getProduits().size(),
                produits,
                promotion.getCodeBarrePack(),
                promotion.getSeuilMontant(),
                promotion.isCumulable(),
                alertes(promotion));
    }
}
