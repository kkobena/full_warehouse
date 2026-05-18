package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.EtatProduitService;
import com.kobe.warehouse.service.ReferenceService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
 * <p>Pour chaque produit éligible (actif, FP principal défini), ce batch transforme le
 * <em>stock objectif</em> — pré-calculé par {@link SemoisCalculationService} et stocké dans
 * {@link SemoisConfiguration#getStockObjectifCalcule()} — en lignes de {@link Suggestion} de
 * type {@link TypeSuggession#AUTO}, regroupées par fournisseur. Le batch ne recalcule aucune
 * valeur SEMOIS : il consomme uniquement les valeurs autoritaires de {@code semois_configuration}.
 *
 * <p><strong>Protection manuelle :</strong> les lignes dont le flag
 * {@code quantiteModifieeManuel=true} ne sont <em>jamais</em> modifiées ni supprimées par ce batch.
 */
@Service
public class SemoisBatchJobService {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisBatchJobService.class);

    /** Taille maximale d'une clause SQL {@code IN (...)} pour éviter les listes trop volumineuses. */
    private static final int IN_CLAUSE_CHUNK_SIZE = 1000;

    /** Garde anti-concurrence : empêche deux exécutions simultanées du batch. */
    private final AtomicBoolean batchEnCours = new AtomicBoolean(false);

    private final ProduitRepository produitRepository;
    private final SemoisConfigurationRepository semoisConfigurationRepository;
    private final SuggestionRepository suggestionRepository;
    private final SuggestionLineRepository suggestionLineRepository;
    private final OrderLineRepository orderLineRepository;
    private final EtatProduitService etatProduitService;
    private final ReferenceService referenceService;
    private final EntityManager em;

    public SemoisBatchJobService(
        ProduitRepository produitRepository,
        SemoisConfigurationRepository semoisConfigurationRepository,
        SuggestionRepository suggestionRepository,
        SuggestionLineRepository suggestionLineRepository,
        OrderLineRepository orderLineRepository,
        EtatProduitService etatProduitService,
        ReferenceService referenceService,
        EntityManager em
    ) {
        this.produitRepository = produitRepository;
        this.semoisConfigurationRepository = semoisConfigurationRepository;
        this.suggestionRepository = suggestionRepository;
        this.suggestionLineRepository = suggestionLineRepository;
        this.orderLineRepository = orderLineRepository;
        this.etatProduitService = etatProduitService;
        this.referenceService = referenceService;
        this.em = em;
    }

    //  Réintégration automatique des exclusions expirées ─────────────────

    /**
     * Réintègre automatiquement les produits dont l'exclusion temporaire a expiré
     */
    @Transactional
    public void reintegrerExclusionsExpirees() {
        int nb = semoisConfigurationRepository.reintegrerExclusionsExpirees();
        if (nb > 0) {
            LOG.info(
                "[SEMOIS-BATCH] {} produit(s) réintégré(s) automatiquement après expiration de l'exclusion",
                nb);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Batch principal — exécuté par le pipeline JobOrchestrationService
    // ────────────────────────────────────────────────────────────────────────────────

    /**
     * Crée ou met à jour les suggestions SEMOIS pour tous les produits éligibles.
     *
     * <p>Les lignes ayant {@code quantiteModifieeManuel=true} sont préservées — le pharmacien a
     * pris la main, le batch ne les modifie ni ne les supprime.
     */
    @Transactional
    public void creerSuggestionBatch() {
        if (!batchEnCours.compareAndSet(false, true)) {
            LOG.warn("[SEMOIS-BATCH] Batch déjà en cours — exécution ignorée");
            return;
        }
        try {
            doCreerSuggestionBatch();
        } finally {
            batchEnCours.set(false);
        }
    }

    private void doCreerSuggestionBatch() {
        LOG.info("[SEMOIS-BATCH] Démarrage du batch de création des suggestions");
        long debut = System.currentTimeMillis();

        Magasin magasin = em.find(Magasin.class, EntityConstant.DEFAULT_MAGASIN);
        if (magasin == null) {
            LOG.warn("[SEMOIS-BATCH] Aucun magasin par défaut trouvé — batch annulé");
            return;
        }

        // Charger tous les produits éligibles avec leur FP principal et leurs stocks ───
        List<Produit> eligibles = produitRepository.findAllSemoisEligibles(magasin.getId());
        if (eligibles.isEmpty()) {
            LOG.info("[SEMOIS-BATCH] Aucun produit éligible trouvé");
            return;
        }
        Set<Integer> allProduitIds = eligibles.stream().map(Produit::getId)
            .collect(Collectors.toSet());
        LOG.info("[SEMOIS-BATCH] {} produits éligibles chargés", allProduitIds.size());

        // Batch-load SemoisConfiguration ────────────────────────────────────────
        Map<Integer, SemoisConfiguration> semoisConfigByProduitId = semoisConfigurationRepository
            .findByProduitIdIn(allProduitIds)
            .stream()
            .collect(Collectors.toMap(sc -> sc.getProduit().getId(), Function.identity()));

        // Passe unique sur les produits éligibles : index des FP principaux et regroupement
        // par fournisseur (évite de re-streamer la liste plusieurs fois).
        Set<Integer> allFpIds = new HashSet<>();
        Map<Fournisseur, List<Produit>> byFournisseur = new HashMap<>();
        for (Produit p : eligibles) {
            FournisseurProduit fp = p.getFournisseurProduitPrincipal();
            if (fp != null) {
                allFpIds.add(fp.getId());
                byFournisseur.computeIfAbsent(fp.getFournisseur(), k -> new ArrayList<>()).add(p);
            }
        }

        // Stock virtuel (commandes en attente) ──────────────────────────────────
        Map<Integer, Integer> pendingQtyByProduitId = loadPendingOrderQty(allProduitIds);

        // Produits non suggérables (commande REQUESTED/RECEIVED en cours) — batch, 1 appel SQL
        // par chunk au lieu de 2 requêtes par produit.
        Set<Integer> nonSuggerables = new HashSet<>();
        for (Set<Integer> chunk : partition(allProduitIds)) {
            nonSuggerables.addAll(etatProduitService.produitsNonSuggerables(chunk));
        }

        // Batch-load lignes SEMOIS existantes ────────────────────────────────────
        Map<Integer, SuggestionLine> existingLineByFpId = suggestionLineRepository
            .findAllByTypeSuggessionAndFournisseurProduitIdIn(TypeSuggession.AUTO, allFpIds)
            .stream()
            .collect(Collectors.toMap(l -> l.getFournisseurProduit().getId(), Function.identity()));

        List<Suggestion> suggestionsToSave = new ArrayList<>();
        List<SuggestionLine> linesToSave = new ArrayList<>();
        List<SuggestionLine> linesToDelete = new ArrayList<>();
        int nbCreees = 0;
        int nbMajees = 0;
        int nbProtegees = 0;

        for (Map.Entry<Fournisseur, List<Produit>> entry : byFournisseur.entrySet()) {
            Fournisseur fournisseur = entry.getKey();

            AtomicBoolean existing = new AtomicBoolean(false);
            Suggestion suggestion = getOrCreateSemoisSuggestion(fournisseur, magasin, existing);
            boolean alreadyExisted = existing.get();

            for (Produit produit : entry.getValue()) {
                FournisseurProduit fp = produit.getFournisseurProduitPrincipal();
                SemoisConfiguration config = semoisConfigByProduitId.get(produit.getId());

                // Produit exclu temporairement des suggestions SEMOIS : on retire toute ligne
                // AUTO devenue obsolète (sauf si une quantité a été saisie manuellement).
                if (config != null && config.isExcluActif()) {
                    marquerLignePerimee(existingLineByFpId.get(fp.getId()), linesToDelete);
                    continue;
                }

                if (nonSuggerables.contains(produit.getId())) {
                    continue;
                }

                int pendingQty = pendingQtyByProduitId.getOrDefault(produit.getId(), 0);
                int stockPhysique = produit.getStockProduits().stream()
                    .filter(sp -> sp.getStorage().getMagasin().equals(magasin))
                    .mapToInt(StockProduit::getTotalStockQuantity)
                    .sum();
                int stockVirtuel = stockPhysique + pendingQty;

                int stockObjectif = computeStockObjectif(produit, config);

                if (stockVirtuel >= stockObjectif) {
                    // Stock redevenu suffisant : la ligne AUTO obsolète est supprimée
                    // (sauf si le pharmacien a saisi une quantité manuellement).
                    marquerLignePerimee(existingLineByFpId.get(fp.getId()), linesToDelete);
                    continue;
                }

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
                    if (alreadyExisted) {
                        linesToSave.add(newLine);
                    }
                    nbCreees++;
                }
            }

            // Une nouvelle suggestion n'est persistée que si elle a effectivement reçu des lignes.
            // Les suggestions existantes sont des entités managées : la mise à jour de updatedAt
            // (et de leurs lignes) est suivie par le dirty-checking, pas besoin de les collecter.
            if (!alreadyExisted && !suggestion.getSuggestionLines().isEmpty()) {
                suggestionsToSave.add(suggestion);
            }
        }

        suggestionRepository.saveAll(suggestionsToSave);
        suggestionLineRepository.saveAll(linesToSave);
        if (!linesToDelete.isEmpty()) {
            suggestionLineRepository.deleteAll(linesToDelete);
        }
        // Nettoyage des suggestions AUTO devenues vides (toutes lignes supprimées) et jamais
        // traitées par le pharmacien (statut GENEREE).
        int nbVides = suggestionRepository.deleteEmptyAutoSuggestions();

        long duree = System.currentTimeMillis() - debut;
        LOG.info(
            "[SEMOIS-BATCH] Terminé en {}ms — {} créées, {} màj, {} protégées, {} lignes supprimées, {} suggestions vides supprimées",
            duree, nbCreees, nbMajees, nbProtegees, linesToDelete.size(), nbVides);
    }

    /**
     * Ajoute une ligne AUTO à la liste des suppressions si elle existe et n'a pas été modifiée
     * manuellement par le pharmacien. Les saisies manuelles ne sont jamais touchées par le batch.
     */
    private void marquerLignePerimee(SuggestionLine ligne, List<SuggestionLine> linesToDelete) {
        if (ligne != null && !ligne.isQuantiteModifieeManuel()) {
            linesToDelete.add(ligne);
        }
    }

    private Suggestion getOrCreateSemoisSuggestion(Fournisseur fournisseur, Magasin magasin,
        AtomicBoolean existing) {
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

    /**
     * Stock objectif d'un produit = valeur autoritaire pré-calculée par
     * {@code SemoisCalculationService.processBatch()} (VMM pondérée + marge de sécurité + stock
     * de rotation, avec plafond péremption éventuel).
     * <p>
     * Lorsque cet objectif est nul — produit jamais vendu, sans historique (VMM = 0) — on retient
     * le seuil mini / quantité d'appro saisi manuellement par le pharmacien. C'est ce seuil
     * manuel qui exprime l'intention « je veux stocker ce produit » en l'absence de ventes.
     * Sans seuil manuel, le produit n'est pas suggéré (conforme au réappro piloté par la demande).
     */
    private int computeStockObjectif(Produit produit, SemoisConfiguration config) {
        int objectif = (config != null && config.getStockObjectifCalcule() != null)
            ? config.getStockObjectifCalcule()
            : safeInt(produit.getQtySeuilMini());
        if (objectif <= 0) {
            objectif = Math.max(safeInt(produit.getQtySeuilMini()), safeInt(produit.getQtyAppro()));
        }
        return objectif;
    }

    private static int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private Map<Integer, Integer> loadPendingOrderQty(Set<Integer> produitIds) {
        if (produitIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Integer> result = new HashMap<>();
        for (Set<Integer> chunk : partition(produitIds)) {
            for (Object[] row : orderLineRepository.findPendingQtyByProduitIds(chunk)) {
                result.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            }
        }
        return result;
    }

    /**
     * Découpe un ensemble d'IDs en sous-ensembles de taille bornée ({@link #IN_CLAUSE_CHUNK_SIZE})
     * pour éviter les clauses SQL {@code IN (...)} trop volumineuses (limites de paramètres du
     * driver et plans de requête dégradés sur de très grandes listes).
     */
    private static List<Set<Integer>> partition(Set<Integer> ids) {
        List<Integer> list = new ArrayList<>(ids);
        List<Set<Integer>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += IN_CLAUSE_CHUNK_SIZE) {
            chunks.add(new LinkedHashSet<>(
                list.subList(i, Math.min(i + IN_CLAUSE_CHUNK_SIZE, list.size()))));
        }
        return chunks;
    }
}
