package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.ExclusionMotif;
import com.kobe.warehouse.repository.JournalExclusionRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.declaration_ca.dto.IntituleTiersPayantDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalExclusionDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalExclusionParamDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalLigneDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalVenteDTO;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Les journaux de ce qui a été écarté du chiffre d'affaires à déclarer.
 *
 * <p>Les écrans d'exclusion disent ce qui <em>sera</em> écarté ; ceux-ci disent ce qui l'a
 * effectivement été, et ce que cela représente. Sans eux, un pharmacien qui coche un rayon n'a aucun
 * moyen de vérifier ce que sa décision a produit — il ne voit que le total, après coup, dans un état
 * comptable où l'écart ne se rattache à rien.
 *
 * <p>La <strong>marge</strong> figure dans les trois journaux, et c'est le chiffre qui décide : une
 * ligne écartée du CA déclaré a bel et bien été vendue et encaissée. Le pharmacien a besoin de voir
 * ce qu'il gagne réellement sur ce qu'il ne déclare pas — un rayon exclu très margé n'a pas la même
 * portée qu'un rayon écoulé à prix coûtant.
 *
 * <p>Les requêtes vivent dans {@link JournalExclusionRepository} ; ce service ne fait qu'assembler
 * leurs résultats et poser le plafond d'affichage.
 */
@Service
public class JournalExclusionService {

    /**
     * Plafond de lignes rapportées.
     *
     * <p>Un mois d'unités gratuites dépasse couramment le millier de lignes. Au-delà, le tableau
     * cesse d'être consulté ligne à ligne : c'est le bandeau d'indicateurs qui porte l'information,
     * et il est calculé, lui, sur la totalité de la période.
     */
    private static final int MAX_LIGNES = 2000;

    private final JournalExclusionRepository journalExclusionRepository;
    private final StorageService storageService;

    public JournalExclusionService(JournalExclusionRepository journalExclusionRepository, StorageService storageService) {
        this.journalExclusionRepository = journalExclusionRepository;
        this.storageService = storageService;
    }

    // ===== Journaux par ligne : unités gratuites et rayons exclus =====

    /**
     * Les lignes dont les unités gratuites ont été retirées du chiffre d'affaires à déclarer.
     *
     * <p>Seule la valeur des unités gratuites sort du CA ; le reste de la ligne y demeure. C'est le
     * seul des trois journaux où le montant exclu est inférieur à la valeur TTC de la ligne.
     */
    @Transactional(readOnly = true)
    public JournalExclusionDTO unitesGratuites(JournalExclusionParamDTO param) {
        return journalDeLignes(param, ExclusionMotif.UG);
    }

    /** Les lignes de produits rattachés à un rayon exclu : la ligne entière sort du CA déclaré. */
    @Transactional(readOnly = true)
    public JournalExclusionDTO rayonsExclus(JournalExclusionParamDTO param) {
        return journalDeLignes(param, ExclusionMotif.RAYON);
    }

    private JournalExclusionDTO journalDeLignes(JournalExclusionParamDTO param, ExclusionMotif motif) {
        String terme = terme(param.recherche());
        List<JournalLigneDTO> lignes = journalExclusionRepository.lignes(
            motif,
            param.dateDebut(),
            param.dateFin(),
            terme,
            magasinId(),
            storagePrincipal().getId(),
            plafond()
        );

        boolean tronque = lignes.size() > MAX_LIGNES;
        return new JournalExclusionDTO(
            journalExclusionRepository.indicateurs(motif, param.dateDebut(), param.dateFin(), terme, magasinId(), null),
            List.of(),
            tronque ? List.copyOf(lignes.subList(0, MAX_LIGNES)) : lignes,
            tronque
        );
    }

    // ===== Journal par vente : tiers-payants exclus =====

    /**
     * Les ventes écartées parce qu'elles relèvent d'un tiers-payant exclu.
     *
     * <p>Le grain est la vente et non la ligne, parce que l'exclusion l'est aussi : le tiers-payant
     * écarte la vente entière. Les lignes se consultent vente par vente, via
     * {@link #lignesDeLaVente} — afficher d'emblée les lignes d'un mois de ventes assurance
     * noierait le fait marquant, qui est de savoir quelles ventes sont sorties et pour combien.
     */
    @Transactional(readOnly = true)
    public JournalExclusionDTO ventesTiersPayant(JournalExclusionParamDTO param) {
        String terme = terme(param.recherche());
        List<JournalVenteDTO> ventes = journalExclusionRepository.ventesTiersPayant(
            ExclusionMotif.TIERS_PAYANT,
            param.dateDebut(),
            param.dateFin(),
            terme,
            magasinId(),
            param.tiersPayantId(),
            plafond()
        );

        boolean tronque = ventes.size() > MAX_LIGNES;
        List<JournalVenteDTO> retenues = tronque ? ventes.subList(0, MAX_LIGNES) : ventes;

        return new JournalExclusionDTO(
            journalExclusionRepository.indicateurs(
                ExclusionMotif.TIERS_PAYANT,
                param.dateDebut(),
                param.dateFin(),
                terme,
                magasinId(),
                param.tiersPayantId()
            ),
            nommerLesTiersPayants(retenues),
            List.of(),
            tronque
        );
    }

    /**
     * Rattache à chaque vente les intitulés de ses tiers-payants.
     *
     * <p>Une seconde requête plutôt qu'une jointure : une vente peut relever de deux organismes, et
     * la joindre à l'agrégat la dupliquerait — donc doublerait ses montants. Le coût est celui d'un
     * aller-retour, pas d'une requête par ligne.
     */
    private List<JournalVenteDTO> nommerLesTiersPayants(List<JournalVenteDTO> ventes) {
        if (ventes.isEmpty()) {
            return List.of();
        }
        List<Long> identifiants = ventes.stream().map(JournalVenteDTO::saleId).toList();

        Map<Long, String> parVente = new LinkedHashMap<>();
        for (IntituleTiersPayantDTO rattachement : journalExclusionRepository.nomsTiersPayant(identifiants)) {
            parVente.merge(rattachement.saleId(), rattachement.intitule(), (existant, ajout) ->
                existant.contains(ajout) ? existant : existant + ", " + ajout
            );
        }

        return ventes.stream().map(vente -> vente.avecTiersPayants(parVente.get(vente.saleId()))).toList();
    }

    /** Le détail d'une vente tiers-payant exclue, chargé à l'ouverture du panneau. */
    @Transactional(readOnly = true)
    public List<JournalLigneDTO> lignesDeLaVente(Long saleId, LocalDate saleDate) {
        return journalExclusionRepository.lignesDeLaVente(saleId, saleDate, ExclusionMotif.TIERS_PAYANT, magasinId());
    }

    // ===== Paramètres =====

    /**
     * Le magasin de l'utilisateur connecté.
     *
     * <p>Les journaux ne montrent jamais les ventes d'une autre officine : sur une installation
     * multi-magasins, l'absence de ce filtre laissait chacun lire le chiffre d'affaires des autres.
     */
    private Integer magasinId() {
        return storagePrincipal().getMagasin().getId();
    }

    /** Le dépôt principal de ce magasin : c'est lui qui porte les rayons dont dépend l'exclusion. */
    private Storage storagePrincipal() {
        return storageService.getDefaultConnectedUserMainStorage();
    }

    /** Une ligne de plus que le plafond : c'est elle qui révèle qu'il en existe d'autres. */
    private static Pageable plafond() {
        return PageRequest.of(0, MAX_LIGNES + 1);
    }

    /**
     * Recherche insensible à la casse et non ancrée : le pharmacien tape un fragment, pas un code
     * entier. {@code null} quand rien n'est saisi — la requête sait alors ne pas filtrer.
     */
    private static String terme(String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return null;
        }
        return "%" + recherche.trim().toUpperCase() + "%";
    }
}
