package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
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
import com.kobe.warehouse.service.scheduler.dto.SemoisEligibleItem;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch nocturne SEMOIS — crée et rafraîchit les suggestions de réapprovisionnement.
 *
 * <p>Pour chaque produit éligible (actif, FP principal défini), ce batch transforme le
 * <em>stock objectif</em> — pré-calculé par {@link SemoisCalculationService} et stocké dans
 * {@code SemoisConfiguration#getStockObjectifCalcule()} — en lignes de {@link Suggestion} de
 * type {@link TypeSuggession#AUTO}, regroupées par fournisseur. Le batch ne recalcule aucune
 * valeur SEMOIS : il consomme uniquement les valeurs autoritaires de {@code semois_configuration}.
 *
 * <p><strong>Protection manuelle :</strong> les lignes dont le flag
 * {@code quantiteModifieeManuel=true} ne sont <em>jamais</em> modifiées ni supprimées par ce batch.
 *
 * <p><strong>Scalabilité :</strong> les produits sont traités page par page ({@value #SEMOIS_PAGE_SIZE}
 * produits/page). Un {@code em.flush()/clear()} entre chaque page libère le cache de premier
 * niveau et borne la consommation mémoire à O(page) plutôt que O(catalogue).
 */
@Service
public class SemoisBatchJobService {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisBatchJobService.class);

    /** Nombre de produits traités par page. */
    private static final int SEMOIS_PAGE_SIZE = 500;

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
    // Nettoyage — à planifier indépendamment (hebdomadaire ou après désactivation produit)
    // ────────────────────────────────────────────────────────────────────────────────

    /**
     * Supprime les lignes de suggestions AUTO non encore validées dont le produit a été désactivé
     * ou est de type DETAIL, puis efface les suggestions devenues vides.
     *
     * <p>À appeler régulièrement (ex : hebdomadaire) ou manuellement après une désactivation
     * de produits en masse. N'affecte pas les lignes modifiées manuellement par le pharmacien.
     *
     * @return nombre total de lignes et de suggestions supprimées
     */
    @Transactional
    public int nettoyerSuggestionsObsoletes() {
        int nbLignes = suggestionLineRepository.deleteAutoLinesForInactive();
        int nbSugg = suggestionRepository.deleteEmptyAutoSuggestions();
        LOG.info("[SEMOIS-BATCH] Nettoyage : {} lignes obsolètes supprimées, {} suggestions vides supprimées",
            nbLignes, nbSugg);
        return nbLignes + nbSugg;
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

    @Transactional
    public void doCreerSuggestionBatch() {
        LOG.info("[SEMOIS-BATCH] Démarrage du batch de création des suggestions");
        long debut = System.currentTimeMillis();

        Magasin magasin = em.find(Magasin.class, EntityConstant.DEFAULT_MAGASIN);
        if (magasin == null) {
            LOG.warn("[SEMOIS-BATCH] Aucun magasin par défaut trouvé — batch annulé");
            return;
        }
        Integer magasinId = magasin.getId();

        int pageIndex = 0;
        boolean hasNext = true;
        int nbCreees = 0;
        int nbMajees = 0;
        int nbProtegees = 0;
        int nbLignesSupp = 0;
        int totalProduits = 0;

        while (hasNext) {
            Pageable pageable = PageRequest.of(pageIndex, SEMOIS_PAGE_SIZE);
            Slice<SemoisEligibleItem> page = produitRepository.findSemoisEligibleItemsSlice(magasinId, pageable);
            List<SemoisEligibleItem> items = page.getContent();
            hasNext = page.hasNext();
            pageIndex++;

            if (items.isEmpty()) {
                break;
            }
            totalProduits += items.size();

            // Collect IDs for this page
            Set<Integer> produitIds = new HashSet<>();
            Set<Integer> fpIds = new HashSet<>();
            for (SemoisEligibleItem item : items) {
                produitIds.add(item.produitId());
                fpIds.add(item.fpPrincipalId());
            }

            // Batch-load non-suggerables and pending quantities for this page
            Set<Integer> nonSuggerables = new HashSet<>();
            for (Set<Integer> chunk : partition(produitIds)) {
                nonSuggerables.addAll(etatProduitService.produitsNonSuggerables(chunk));
            }
            Map<Integer, Integer> pendingQtyByProduitId = loadPendingOrderQty(produitIds);

            // Batch-load existing AUTO suggestion lines for this page's FP IDs
            Map<Integer, SuggestionLine> existingLineByFpId = suggestionLineRepository
                .findAllByTypeSuggessionAndFournisseurProduitIdIn(TypeSuggession.AUTO, fpIds)
                .stream()
                .collect(Collectors.toMap(
                    l -> l.getFournisseurProduit().getId(),
                    Function.identity(),
                    (a, b) -> a
                ));

            // Group items by fournisseur for suggestion management
            Map<Integer, List<SemoisEligibleItem>> byFournisseurId = items.stream()
                .collect(Collectors.groupingBy(SemoisEligibleItem::fournisseurId));

            List<Suggestion> suggestionsToSave = new ArrayList<>();
            List<SuggestionLine> linesToSave = new ArrayList<>();
            List<SuggestionLine> linesToDelete = new ArrayList<>();

            for (Map.Entry<Integer, List<SemoisEligibleItem>> entry : byFournisseurId.entrySet()) {
                Integer fournisseurId = entry.getKey();

                Optional<Suggestion> optSuggestion = suggestionRepository
                    .findByTypeSuggessionAndFournisseurIdAndMagasinId(TypeSuggession.AUTO, fournisseurId, magasinId);
                final boolean alreadyExisted = optSuggestion.isPresent();
                final Suggestion suggestion;
                if (alreadyExisted) {
                    suggestion = optSuggestion.get();
                    suggestion.setUpdatedAt(LocalDateTime.now());
                } else {
                    suggestion = buildNewSemoisSuggestion(
                        em.getReference(Fournisseur.class, fournisseurId),
                        em.getReference(Magasin.class, magasinId)
                    );
                }

                for (SemoisEligibleItem item : entry.getValue()) {
                    if (item.isExcluActif()) {
                        marquerLignePerimee(existingLineByFpId.get(item.fpPrincipalId()), linesToDelete);
                        continue;
                    }

                    if (nonSuggerables.contains(item.produitId())) {
                        continue;
                    }

                    int pendingQty = pendingQtyByProduitId.getOrDefault(item.produitId(), 0);
                    int stockVirtuel = (int) item.totalStock() + pendingQty;
                    int stockObjectif = computeStockObjectif(item);

                    if (stockVirtuel >= stockObjectif) {
                        marquerLignePerimee(existingLineByFpId.get(item.fpPrincipalId()), linesToDelete);
                        continue;
                    }

                    int qtyBrute = Math.max(1, stockObjectif - stockVirtuel);
                    int qty = item.appliquerColisage(qtyBrute);

                    SuggestionLine existingLine = existingLineByFpId.get(item.fpPrincipalId());
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
                        newLine.setFournisseurProduit(em.getReference(FournisseurProduit.class, item.fpPrincipalId()));
                        newLine.setSuggestion(suggestion);
                        if (alreadyExisted) {
                            linesToSave.add(newLine);
                        } else {
                            suggestion.getSuggestionLines().add(newLine);
                        }
                        nbCreees++;
                    }
                }

                if (!alreadyExisted && !suggestion.getSuggestionLines().isEmpty()) {
                    suggestionsToSave.add(suggestion);
                }
            }

            suggestionRepository.saveAll(suggestionsToSave);
            suggestionLineRepository.saveAll(linesToSave);
            if (!linesToDelete.isEmpty()) {
                suggestionLineRepository.deleteAll(linesToDelete);
                nbLignesSupp += linesToDelete.size();
            }
            em.flush();
            em.clear();

            LOG.debug("[SEMOIS-BATCH] Page {} ({} produits) — créées:{} màj:{} prot:{} suppr:{}",
                pageIndex, items.size(), nbCreees, nbMajees, nbProtegees, nbLignesSupp);
        }

        if (totalProduits == 0) {
            LOG.info("[SEMOIS-BATCH] Aucun produit éligible trouvé");
            return;
        }

        // Nettoyage des suggestions AUTO devenues vides (toutes lignes supprimées) et jamais
        // traitées par le pharmacien (statut GENEREE).
        int nbVides = suggestionRepository.deleteEmptyAutoSuggestions();

        long duree = System.currentTimeMillis() - debut;
        LOG.info(
            "[SEMOIS-BATCH] Terminé en {}ms — {} produits, {} créées, {} màj, {} protégées, {} lignes supprimées, {} suggestions vides supprimées",
            duree, totalProduits, nbCreees, nbMajees, nbProtegees, nbLignesSupp, nbVides);
    }

    private Suggestion buildNewSemoisSuggestion(Fournisseur fournisseur, Magasin magasin) {
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
     * Ajoute une ligne AUTO à la liste des suppressions si elle existe et n'a pas été modifiée
     * manuellement par le pharmacien. Les saisies manuelles ne sont jamais touchées par le batch.
     */
    private void marquerLignePerimee(SuggestionLine ligne, List<SuggestionLine> linesToDelete) {
        if (ligne != null && !ligne.isQuantiteModifieeManuel()) {
            linesToDelete.add(ligne);
        }
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
    private int computeStockObjectif(SemoisEligibleItem item) {
        int objectif = item.stockObjectifCalcule() != null
            ? item.stockObjectifCalcule()
            : safeInt(item.qtySeuilMini());
        if (objectif <= 0) {
            objectif =  Math.max(1,Math.max(safeInt(item.qtySeuilMini()), safeInt(item.qtyAppro()))) ;
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
