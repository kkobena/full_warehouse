package com.kobe.warehouse.service.referentiel;

import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ProduitDci;
import com.kobe.warehouse.domain.ProduitRefSpecialite;
import com.kobe.warehouse.domain.RefSpecialite;
import com.kobe.warehouse.domain.enumeration.DecisionRapprochement;
import com.kobe.warehouse.domain.enumeration.NatureSubstance;
import com.kobe.warehouse.domain.enumeration.StatutRapprochement;
import com.kobe.warehouse.repository.DciRepository;
import com.kobe.warehouse.repository.ProduitRefSpecialiteRepository;
import com.kobe.warehouse.repository.RefSpecialiteCompositionRepository;
import com.kobe.warehouse.repository.RefSpecialiteRcpRepository;
import com.kobe.warehouse.service.errors.GenericError;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rapprochement des produits du catalogue avec le référentiel médicament (tables {@code ref_*}).
 *
 * <p>Le calcul n'est PAS fait ici : il vit dans PostgreSQL (fonctions {@code ref_automatiser_rapprochement},
 * {@code ref_lier_dci}, migration V2.1.20), en une requête ensembliste et une transaction. Les
 * propositions sûres (SUR, PAR_DCI) sont acceptées d'office et posent les DCI des produits qui n'en
 * ont pas ; la relecture humaine ne porte que sur le reste. Cette classe appelle ces fonctions, et
 * porte la lecture de la fiche référentiel et les décisions manuelles.
 */
@Service
public class RapprochementProduitService {

    private static final Logger LOG = LoggerFactory.getLogger(RapprochementProduitService.class);

    private final EntityManager entityManager;
    private final ProduitRefSpecialiteRepository repository;
    private final DciRepository dciRepository;
    private final RefSpecialiteCompositionRepository compositionRepository;
    private final RefSpecialiteRcpRepository rcpRepository;

    public RapprochementProduitService(
        EntityManager entityManager,
        ProduitRefSpecialiteRepository repository,
        DciRepository dciRepository,
        RefSpecialiteCompositionRepository compositionRepository,
        RefSpecialiteRcpRepository rcpRepository
    ) {
        this.entityManager = entityManager;
        this.repository = repository;
        this.dciRepository = dciRepository;
        this.compositionRepository = compositionRepository;
        this.rcpRepository = rcpRepository;
    }

    /** Produits actifs encore sans proposition : le passage de nuit. */
    @Transactional
    public int rapprocherNouveauxProduits() {
        return appelerFonction("SELECT ref_automatiser_rapprochement(NULL, FALSE)", null);
    }

    /**
     * Recalcule aussi les propositions EN_ATTENTE : à lancer après le rechargement du référentiel.
     * Les décisions VALIDE et REJETE ne sont jamais touchées.
     */
    @Transactional
    public int recalculerPropositions() {
        return appelerFonction("SELECT ref_automatiser_rapprochement(NULL, TRUE)", null);
    }

    /** Recalcule les propositions EN_ATTENTE des produits donnés (liste vide : rien). */
    @Transactional
    public int rapprocherProduits(Collection<Integer> produitIds) {
        if (produitIds == null || produitIds.isEmpty()) {
            return 0;
        }
        return appelerFonction("SELECT ref_automatiser_rapprochement(CAST(:ids AS integer[]), TRUE)", formaterTableau(produitIds));
    }

    /** Relie les DCI ajoutées au référentiel, puis rapproche les produits dont le libellé commence par l'une d'elles. */
    @Transactional
    public int rapprocherApresAjoutDci(Collection<Integer> dciIds) {
        if (dciIds == null || dciIds.isEmpty()) {
            return 0;
        }
        int ecrites = appelerFonction("SELECT ref_rapprocher_apres_nouvelles_dci(CAST(:ids AS integer[]))", formaterTableau(dciIds));
        LOG.info("[REFERENTIEL] {} DCI ajoutée(s) : {} proposition(s) de rapprochement recalculée(s)", dciIds.size(), ecrites);
        return ecrites;
    }

    /**
     * Valide une proposition. Si le produit n'a encore aucune DCI, il reçoit celles de la spécialité
     * retenue qui sont reliées au catalogue ; un produit déjà doté de DCI n'est jamais modifié.
     */
    @Transactional
    public ProduitRefSpecialite valider(Integer produitId) {
        ProduitRefSpecialite proposition = chargerEnAttente(produitId);
        if (proposition.getSpecialite() == null) {
            throw new GenericError("Aucune spécialité à valider pour ce produit.", "rapprochementSansSpecialite");
        }
        proposition.valider();
        Produit produit = proposition.getProduit();
        if (produit.getDcis().isEmpty()) {
            List<Dci> dcis = chargerDcisDeLaSpecialite(proposition.getSpecialite().getCis());
            if (!dcis.isEmpty()) {
                produit.remplacerDcis(dcis);
            }
        }
        return proposition;
    }

    /**
     * Rejette un rapprochement, y compris un rapprochement accepté d'office. Les DCI déjà posées
     * restent : elles se retirent sur la fiche produit, ce qui rejette aussi le rapprochement.
     */
    @Transactional
    public ProduitRefSpecialite rejeter(Integer produitId) {
        return chargerProposition(produitId).rejeter();
    }

    /** Oublie la proposition d'un produit : le prochain passage la recalcule (le produit redevient candidat). */
    @Transactional
    public void reinitialiser(Integer produitId) {
        repository.delete(chargerProposition(produitId));
    }

    /** File de relecture : ce que les règles n'ont pas pu accepter seules, les plus sûrs d'abord. */
    @Transactional(readOnly = true)
    public Page<RapprochementDTO> listerRapprochementsARelire(Pageable pageable) {
        return repository
            .findByDecisionAndStatutInOrderByScoreDescIdAsc(
                DecisionRapprochement.EN_ATTENTE,
                List.of(StatutRapprochement.A_VERIFIER, StatutRapprochement.PAR_DCI),
                pageable
            )
            .map(RapprochementDTO::creerDepuis);
    }

    /**
     * La fiche référentiel d'un produit, pour le comptoir : spécialité, molécules et dosages, RCP
     * (indications, posologie, contre-indications) et substituts du catalogue. Rien de tout cela
     * n'est servi tant que le rapprochement n'est pas de confiance (AUTO ou VALIDE).
     */
    @Transactional(readOnly = true)
    public ProduitReferentielDTO lireFiche(Integer produitId) {
        ProduitRefSpecialite r = repository.findByProduitId(produitId).orElse(null);
        if (r == null) {
            return ProduitReferentielDTO.creerSansReferentiel(produitId);
        }
        boolean confiance =
            r.getSpecialite() != null &&
            (r.getDecision() == DecisionRapprochement.AUTO || r.getDecision() == DecisionRapprochement.VALIDE);
        if (!confiance) {
            return new ProduitReferentielDTO(produitId, r.getStatut(), r.getDecision(), r.getScore(), r.getMotif(), false, null, List.of(), null, List.of());
        }
        RefSpecialite s = r.getSpecialite();
        var specialite = new ProduitReferentielDTO.Specialite(
            s.getCis(),
            s.getLibelle(),
            s.getForme(),
            s.getVoies(),
            s.getTitulaire(),
            s.isCommercialisee(),
            s.getTypeGenerique(),
            s.getGroupeGenerique() == null ? null : s.getGroupeGenerique().getLibelle(),
            s.getPrincepsDuGroupe()
        );
        var molecules = compositionRepository
            .findByCis(s.getCis())
            .stream()
            .map(c ->
                new ProduitReferentielDTO.Molecule(
                    c.getSubstance().getLibelle(),
                    c.getDci() == null ? null : c.getDci().getLibelle(),
                    c.getNature().name(),
                    c.getDosageTexte(),
                    c.getReferenceDosage()
                )
            )
            .toList();
        var rcp = rcpRepository
            .findById(s.getCis())
            .map(x -> new ProduitReferentielDTO.Rcp(x.getIndications(), x.getPosologie(), x.getContreIndications()))
            .orElse(null);
        var substituts = s.getGroupeGenerique() == null
            ? List.<ProduitReferentielDTO.Substitut>of()
            : repository.findSubstituts(s.getGroupeGenerique().getId(), produitId);
        return new ProduitReferentielDTO(produitId, r.getStatut(), r.getDecision(), r.getScore(), r.getMotif(), r.isDciPosee(), specialite, molecules, rcp, substituts);
    }

    private ProduitRefSpecialite chargerProposition(Integer produitId) {
        return repository
            .findByProduitId(produitId)
            .orElseThrow(() -> new GenericError("Aucune proposition pour ce produit.", "rapprochementIntrouvable"));
    }

    private ProduitRefSpecialite chargerEnAttente(Integer produitId) {
        ProduitRefSpecialite proposition = chargerProposition(produitId);
        if (proposition.getDecision() != DecisionRapprochement.EN_ATTENTE) {
            throw new GenericError("Cette proposition a déjà été traitée.", "rapprochementDejaTraite");
        }
        return proposition;
    }

    /** DCI du catalogue de la spécialité : celles de la fraction thérapeutique, à défaut de la substance active. */
    private List<Dci> chargerDcisDeLaSpecialite(String cis) {
        List<Integer> ids = repository.findCatalogueDciIds(cis, NatureSubstance.FT);
        if (ids.isEmpty()) {
            ids = repository.findCatalogueDciIds(cis, NatureSubstance.SA);
        }
        return dciRepository.findAllById(ids);
    }

    private int appelerFonction(String sql, String ids) {
        var query = entityManager.createNativeQuery(sql);
        if (ids != null) {
            query.setParameter("ids", ids);
        }
        int n = ((Number) query.getSingleResult()).intValue();
        if (n > 0) {
            // Les fonctions SQL écrivent produit_dci et produit sans passer par Hibernate : sans cela, le
            // cache de second niveau servirait encore les produits sans DCI.
            var cache = entityManager.getEntityManagerFactory().getCache();
            cache.evict(Produit.class);
            cache.evict(ProduitDci.class);
        }
        return n;
    }

    /** Littéral de tableau PostgreSQL : « {1,2,3} ». */
    private static String formaterTableau(Collection<Integer> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(",", "{", "}"));
    }
}
