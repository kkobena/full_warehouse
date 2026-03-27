package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SemoisClasseConfig;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SemoisClasseConfigRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.EtatProduitService;
import com.kobe.warehouse.service.ReferenceService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch nocturne SEMOIS — crée et rafraîchit les suggestions de réapprovisionnement.
 *
 * <p>Exécuté chaque nuit à 02h00. Pour chaque produit éligible (modèle=SEMOIS, actif,
 * FP principal défini), recalcule le VMM et le stock objectif, puis crée/met à jour la
 * {@link Suggestion} de type {@link TypeSuggession#AUTO} pour chaque fournisseur.
 *
 * <p><strong>Protection manuelle:</strong> les lignes dont le flag
 * {@code quantiteModifieeManuel=true} ne sont <em>pas</em> modifiées par ce batch.
 */
@Service
public class SemoisBatchJobService {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisBatchJobService.class);

    private final ProduitRepository produitRepository;
    private final SemoisConfigurationRepository semoisConfigurationRepository;
    private final SemoisClasseConfigRepository semoisClasseConfigRepository;
    private final SuggestionRepository suggestionRepository;
    private final SuggestionLineRepository suggestionLineRepository;
    private final OrderLineRepository orderLineRepository;
    private final EtatProduitService etatProduitService;
    private final ReferenceService referenceService;
    private final EntityManager em;

    public SemoisBatchJobService(
        ProduitRepository produitRepository,
        SemoisConfigurationRepository semoisConfigurationRepository,
        SemoisClasseConfigRepository semoisClasseConfigRepository,
        SuggestionRepository suggestionRepository,
        SuggestionLineRepository suggestionLineRepository,
        OrderLineRepository orderLineRepository,
        EtatProduitService etatProduitService,
        ReferenceService referenceService,
        EntityManager em
    ) {
        this.produitRepository = produitRepository;
        this.semoisConfigurationRepository = semoisConfigurationRepository;
        this.semoisClasseConfigRepository = semoisClasseConfigRepository;
        this.suggestionRepository = suggestionRepository;
        this.suggestionLineRepository = suggestionLineRepository;
        this.orderLineRepository = orderLineRepository;
        this.etatProduitService = etatProduitService;
        this.referenceService = referenceService;
        this.em = em;
    }

    // ── S4.3 — Réintégration automatique des exclusions expirées ─────────────────
    /**
     * Réintègre automatiquement les produits dont l'exclusion temporaire a expiré.
     * Exécuté chaque nuit à 01h30 (avant le batch principal à 02h00).
     */
    @Transactional
    public void reintegrerExclusionsExpirees() {
        int nb = semoisConfigurationRepository.reintegrerExclusionsExpirees();
        if (nb > 0) {
            LOG.info("[SEMOIS-BATCH] {} produit(s) réintégré(s) automatiquement après expiration de l'exclusion", nb);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Batch principal — planifié à 02h00 chaque nuit
    // ────────────────────────────────────────────────────────────────────────────────

    /**
     * Crée ou met à jour les suggestions SEMOIS pour tous les produits éligibles.
     *
     * <p>Les lignes ayant {@code quantiteModifieeManuel=true} sont
     * préservées — le pharmacien a pris la main, le batch ne les modifie pas.
     * Seul un appel explicite à {@code resetQuantiteManuelle()} réautorise le batch.
     */
    @Transactional
    public void creerSuggestionBatch() {
        LOG.info("[SEMOIS-BATCH] Démarrage du batch de création des suggestions");
        long debut = System.currentTimeMillis();

        Magasin magasin = em.find(Magasin.class, EntityConstant.DEFAULT_MAGASIN);
        if (magasin == null) {
            LOG.warn("[SEMOIS-BATCH] Aucun magasin par défaut trouvé — batch annulé");
            return;
        }

        // Charger les configs de classe (au plus 5 lignes) ─────────────────────
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigs = semoisClasseConfigRepository.findAll()
            .stream()
            .collect(Collectors.toMap(SemoisClasseConfig::getClasseCriticite, Function.identity()));

        //Charger tous les produits éligibles avec leur FP principal ────────────
        List<Produit> eligibles = produitRepository.findAllSemoisEligibles(magasin.getId());
        if (eligibles.isEmpty()) {
            LOG.info("[SEMOIS-BATCH] Aucun produit éligible trouvé");
            return;
        }

        Set<Integer> allProduitIds = eligibles.stream().map(Produit::getId).collect(Collectors.toSet());
        LOG.info("[SEMOIS-BATCH] {} produits éligibles chargés", allProduitIds.size());

        // Batch-load SemoisConfiguration ────────────────────────────────────────
        Map<Integer, SemoisConfiguration> semoisConfigByProduitId = semoisConfigurationRepository
            .findByProduitIdIn(allProduitIds)
            .stream()
            .collect(Collectors.toMap(sc -> sc.getProduit().getId(), Function.identity()));

        // Batch VMM par classe ──────────────────────────────────────────────────
        Map<Integer, Integer> vmmByProduitId = loadVmmBySemoisClass(eligibles, classeConfigs);

        // Stock virtuel (commandes en attente) ──────────────────────────────────
        Map<Integer, Integer> pendingQtyByProduitId = loadPendingOrderQty(allProduitIds);

        // Batch-load lignes SEMOIS existantes ────────────────────────────────────
        Set<Integer> allFpIds = eligibles.stream()
            .filter(p -> p.getFournisseurProduitPrincipal() != null)
            .map(p -> p.getFournisseurProduitPrincipal().getId())
            .collect(Collectors.toSet());

        Map<Integer, SuggestionLine> existingLineByFpId = suggestionLineRepository
            .findAllByTypeSuggessionAndFournisseurProduitIdIn(TypeSuggession.AUTO, allFpIds)
            .stream()
            .collect(Collectors.toMap(l -> l.getFournisseurProduit().getId(), Function.identity()));

        // Regroupement par fournisseur ──────────────────────────────────────────
        Map<Fournisseur, List<Produit>> byFournisseur = eligibles.stream()
            .filter(p -> p.getFournisseurProduitPrincipal() != null)
            .collect(Collectors.groupingBy(p -> p.getFournisseurProduitPrincipal().getFournisseur()));

        List<Suggestion> suggestionsToSave = new ArrayList<>();
        List<SuggestionLine> linesToSave = new ArrayList<>();
        List<SemoisConfiguration> configsToUpdate = new ArrayList<>();
        int nbCreees = 0;
        int nbMajees = 0;
        int nbProtegees = 0;

        for (Map.Entry<Fournisseur, List<Produit>> entry : byFournisseur.entrySet()) {
            Fournisseur fournisseur = entry.getKey();

            AtomicBoolean existing = new AtomicBoolean(false);
            Suggestion suggestion = getOrCreateSemoisSuggestion(fournisseur, magasin, existing);
            boolean alreadyExisted = existing.get();

            for (Produit produit : entry.getValue()) {
                if (!etatProduitService.canSuggere(produit.getId())) continue;

                FournisseurProduit fp = produit.getFournisseurProduitPrincipal();
                SemoisConfiguration config = semoisConfigByProduitId.get(produit.getId());
                int vmm = vmmByProduitId.getOrDefault(produit.getId(), 0);
                int pendingQty = pendingQtyByProduitId.getOrDefault(produit.getId(), 0);

                int stockPhysique = produit.getStockProduits().stream()
                    .filter(sp -> sp.getStorage().getMagasin().equals(magasin))
                    .mapToInt(StockProduit::getTotalStockQuantity)
                    .sum();
                int stockVirtuel = stockPhysique + pendingQty;

                int stockObjectif = computeStockObjectif(produit, config, fournisseur, vmm, classeConfigs);

                // Mise à jour du cache stockObjectifCalcule
                if (config != null) {
                    config.setStockObjectifCalcule(stockObjectif);
                    config.setDateDernierCalcul(LocalDateTime.now());
                    configsToUpdate.add(config);
                }

                if (stockVirtuel >= stockObjectif) continue;

                // Quantité brute = besoin pour atteindre le stock objectif
                int qtyBrute = Math.max(1, stockObjectif - stockVirtuel);
                //Arrondi au colisage fournisseur (multiple de colis + qté minimale)
                int qty = fp.appliquerColisage(qtyBrute);

                SuggestionLine existingLine = existingLineByFpId.get(fp.getId());
                if (existingLine != null) {
                    if (existingLine.isQuantiteModifieeManuel()) {
                        LOG.debug("[SEMOIS-BATCH] Ligne {} protégée (qté manuelle={}) — skip",
                            existingLine.getId(), existingLine.getQuantity());
                        nbProtegees++;
                        continue;
                    }
                    existingLine.setQuantity(qty);
                    existingLine.setUpdatedAt(LocalDateTime.now());
                    linesToSave.add(existingLine);
                    nbMajees++;
                } else {
                    SuggestionLine newLine = new SuggestionLine();
                    newLine.setCreatedAt(LocalDateTime.now());
                    newLine.setUpdatedAt(newLine.getCreatedAt());
                    newLine.setQuantity(qty);
                    newLine.setFournisseurProduit(fp);
                    newLine.setSuggestion(suggestion);
                    suggestion.getSuggestionLines().add(newLine);
                    if (alreadyExisted) linesToSave.add(newLine);
                    nbCreees++;
                }
            }

            if (!suggestion.getSuggestionLines().isEmpty() || !linesToSave.isEmpty()) {
                suggestionsToSave.add(suggestion);
            }
        }


        semoisConfigurationRepository.saveAll(configsToUpdate);
        suggestionRepository.saveAll(suggestionsToSave);
        suggestionLineRepository.saveAll(linesToSave);

        long duree = System.currentTimeMillis() - debut;
        LOG.info("[SEMOIS-BATCH] Terminé en {}ms — {} suggestions, {} créées, {} màj, {} protégées",
            duree, suggestionsToSave.size(), nbCreees, nbMajees, nbProtegees);
    }


    private Suggestion getOrCreateSemoisSuggestion(Fournisseur fournisseur, Magasin magasin, AtomicBoolean existing) {
        Optional<Suggestion> opt = suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(
            TypeSuggession.AUTO,
            fournisseur.getId(),
            magasin.getId()
        );
        if (opt.isPresent()) {
            existing.set(true);
            Suggestion s = opt.get();
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        }
        existing.set(false);
        Suggestion s = new Suggestion()
            .setSuggessionReference(
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    .concat(referenceService.buildSuggestionReference())
            )
            .createdAt(LocalDateTime.now());
        s.setFournisseur(fournisseur);
        s.setMagasin(magasin);
        s.setTypeSuggession(TypeSuggession.AUTO);
        s.setStatut(StatutSuggession.GENEREE);
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private int computeStockObjectif(
        Produit produit,
        SemoisConfiguration config,
        Fournisseur fournisseur,
        int vmm,
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigs
    ) {
        if (vmm <= 0) return produit.getQtySeuilMini();

        ClasseCriticite classe = produit.getEffectiveClasseCriticite();
        SemoisClasseConfig cc = classeConfigs.get(classe);
        int delai = resolveDelaiLivraison(config, fournisseur);

        if (cc == null) {
            return (int) Math.round(vmm * (1 + delai / 30.0));
        }
        BigDecimal coeff = cc.getCoefficientSecurite();
        int marge = BigDecimal.valueOf(vmm)
            .multiply(BigDecimal.valueOf(delai))
            .multiply(coeff)
            .divide(BigDecimal.valueOf(30), RoundingMode.HALF_UP)
            .intValue();
        return vmm + marge;
    }

    private int resolveDelaiLivraison(SemoisConfiguration config, Fournisseur fournisseur) {
        if (config != null && config.getDelaiLivraisonJours() != null) {
            return config.getDelaiLivraisonJours();
        }
        if (fournisseur != null && fournisseur.getDelaiLivraisonJours() != null) {
            return fournisseur.getDelaiLivraisonJours();
        }
        if (fournisseur != null
            && fournisseur.getGroupeFournisseur() != null
            && fournisseur.getGroupeFournisseur().getDelaiLivraisonJours() != null) {
            return fournisseur.getGroupeFournisseur().getDelaiLivraisonJours();
        }
        return 7;
    }

    private Map<Integer, Integer> loadVmmBySemoisClass(
        List<Produit> produits,
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigs
    ) {
        Map<ClasseCriticite, List<Integer>> idsByClass = produits.stream()
            .collect(Collectors.groupingBy(
                Produit::getEffectiveClasseCriticite,
                Collectors.mapping(Produit::getId, Collectors.toList())
            ));

        Map<Integer, Integer> result = new HashMap<>();
        for (Map.Entry<ClasseCriticite, List<Integer>> e : idsByClass.entrySet()) {
            SemoisClasseConfig cc = classeConfigs.get(e.getKey());
            int nbMois = cc != null ? cc.getNbMoisHistorique() : 6;
            result.putAll(loadVmm(new HashSet<>(e.getValue()), nbMois));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> loadVmm(Set<Integer> produitIds, int nthMois) {
        if (produitIds.isEmpty()) return Map.of();
        String sql = """
            SELECT produit_id, ROUND(AVG(qte_vendue))::integer AS vmm
            FROM mv_monthly_top_products
            WHERE produit_id IN (:produitIds)
              AND mois::date >= (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '%d months')::date
              AND mois::date < DATE_TRUNC('month', CURRENT_DATE)::date
            GROUP BY produit_id
            """.formatted(nthMois);
        List<Object[]> rows = em.createNativeQuery(sql)
            .setParameter("produitIds", produitIds)
            .getResultList();
        return rows.stream().collect(Collectors.toMap(
            row -> ((Number) row[0]).intValue(),
            row -> ((Number) row[1]).intValue()
        ));
    }

    private Map<Integer, Integer> loadPendingOrderQty(Set<Integer> produitIds) {
        if (produitIds.isEmpty()) return Map.of();
        List<Object[]> rows = orderLineRepository.findPendingQtyByProduitIds(produitIds);
        return rows.stream().collect(Collectors.toMap(
            row -> ((Number) row[0]).intValue(),
            row -> ((Number) row[1]).intValue()
        ));
    }
}
