package com.kobe.warehouse.service.facturation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.OrigineGeneration;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.errors.InvoiceEmptyDataException;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les cinq services d'édition de factures tiers payant, sur un vrai PostgreSQL.
 *
 * <p>Éditer des factures, c'est d'abord <em>choisir</em> : une spécification Criteria décide quels
 * dossiers entrent dans le lot — période de vente, vente close et non annulée, dossier pas encore
 * facturé — puis chaque mode y ajoute son propre filtre. C'est ensuite <em>numéroter</em> : le
 * numéro se déduit de la dernière facture émise, lue en base. C'est enfin <em>agréger</em> : les
 * ventilations par taux de TVA de chaque dossier, stockées en {@code jsonb}, sont sommées taux par
 * taux pour établir les totaux de la facture. Aucune de ces trois étapes ne s'observe hors base.
 */
@DisplayName("Édition des factures tiers payant sur PostgreSQL")
class EditionFactureIntegrationTest extends AbstractFacturationIntegrationTest {

    @Test
    @DisplayName("L'édition groupe les dossiers par organisme, une facture chacun")
    void editionParOrganisme() throws Exception {
        ClientTiersPayant premier = compte(tiersPayant("CNAM"));
        ClientTiersPayant second = compte(tiersPayant("MUGEF"));
        dossier(premier, 30_000);
        dossier(premier, 20_000);
        dossier(second, 45_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionAllService.createFactureEdition(parametres(ModeEditionEnum.ALL));
        viderLeCache();

        List<FactureTiersPayant> factures = facturesDe(reponse.generationCode());
        assertEquals(2, factures.size(), "un organisme, une facture");
        assertFalse(reponse.isGroup());
        assertEquals(
            3,
            compter("SELECT count(*) FROM third_party_sale_line WHERE facture_tiers_payant_id IS NOT NULL"),
            "les trois dossiers sont rattachés à leur facture"
        );
    }

    @Test
    @DisplayName("Les totaux de la facture somment les ventilations TVA de ses dossiers")
    void totauxIssusDesRepartitions() throws Exception {
        ClientTiersPayant compte = compte(tiersPayant("CNPS"));
        dossier(compte, 100_000, LocalDate.now(), List.of(repartition(100_000, 0)));
        dossier(compte, 50_000, LocalDate.now(), List.of(repartition(50_000, 0)));
        viderLeCache();

        FactureEditionResponse reponse = services.editionAllService.createFactureEdition(parametres(ModeEditionEnum.ALL));
        viderLeCache();

        FactureTiersPayant facture = facturesDe(reponse.generationCode()).getFirst();
        assertEquals(0, BigDecimal.valueOf(150_000).compareTo(facture.getMontantTtc()));
        assertEquals(0, BigDecimal.valueOf(150_000).compareTo(facture.getMontantNet()));
        assertEquals(1, facture.getRepartitions().size(), "un seul taux en jeu, une seule ventilation");
    }

    @Test
    @DisplayName("Deux taux de TVA donnent deux ventilations distinctes sur la facture")
    void ventilationParTauxDeTva() throws Exception {
        ClientTiersPayant compte = compte(tiersPayant("MULTI TVA"));
        dossier(compte, 60_000, LocalDate.now(), List.of(repartition(40_000, 0), repartition(20_000, 18)));
        dossier(compte, 10_000, LocalDate.now(), List.of(repartition(10_000, 18)));
        viderLeCache();

        FactureEditionResponse reponse = services.editionAllService.createFactureEdition(parametres(ModeEditionEnum.ALL));
        viderLeCache();

        FactureTiersPayant facture = facturesDe(reponse.generationCode()).getFirst();
        assertEquals(2, facture.getRepartitions().size(), "les ventilations sont regroupées par taux, pas par dossier");
        var taux18 = facture.getRepartitions().stream().filter(r -> r.tva() == 18).findFirst().orElseThrow();
        assertEquals(30_000d, taux18.montantTtc(), "20 000 + 10 000 au taux 18");
        assertEquals(0, BigDecimal.valueOf(70_000).compareTo(facture.getMontantTtc()), "le total reprend les deux taux");
    }

    @Test
    @DisplayName("Le numéro de facture reprend la numérotation là où elle s'était arrêtée")
    void numerotationContinue() throws Exception {
        ClientTiersPayant premier = compte(tiersPayant("NUM 1"));
        dossier(premier, 10_000);
        viderLeCache();
        services.editionAllService.createFactureEdition(parametres(ModeEditionEnum.ALL));
        viderLeCache();

        ClientTiersPayant second = compte(tiersPayant("NUM 2"));
        dossier(second, 12_000);
        viderLeCache();
        FactureEditionResponse seconde = services.editionAllService.createFactureEdition(parametres(ModeEditionEnum.ALL));
        viderLeCache();

        FactureTiersPayant facture = facturesDe(seconde.generationCode()).getFirst();
        int annee = LocalDate.now().getYear();
        assertEquals(annee + "_0002", facture.getNumFacture(), "le numéro suit celui de la facture précédente");
    }

    @Test
    @DisplayName("Un lot sans dossier à facturer est refusé plutôt que de créer une facture vide")
    void aucunDossierAFacturer() {
        compte(tiersPayant("SANS DOSSIER"));
        viderLeCache();

        EditionSearchParams params = parametres(ModeEditionEnum.ALL);

        assertThrows(InvoiceEmptyDataException.class, () -> services.editionAllService.createFactureEdition(params));
        assertEquals(0, compter("SELECT count(*) FROM facture_tiers_payant"));
    }

    @Test
    @DisplayName("Un dossier hors de la période reste à facturer")
    void dossierHorsPeriode() throws Exception {
        ClientTiersPayant compte = compte(tiersPayant("PERIODE"));
        ThirdPartySaleLine dansLaPeriode = dossier(compte, 15_000, LocalDate.now());
        ThirdPartySaleLine avant = dossier(compte, 25_000, LocalDate.now().minusMonths(3));
        viderLeCache();

        services.editionAllService.createFactureEdition(parametres(ModeEditionEnum.ALL));
        viderLeCache();

        assertNotNull(em.find(ThirdPartySaleLine.class, dansLaPeriode.getId()).getFactureTiersPayant());
        assertNull(em.find(ThirdPartySaleLine.class, avant.getId()).getFactureTiersPayant(), "hors période, le dossier attend");
    }

    @Test
    @DisplayName("Un dossier déjà porté par une facture définitive n'est pas refacturé")
    void dossierDejaFacture() throws Exception {
        ClientTiersPayant compte = compte(tiersPayant("DEJA FACTURE"));
        ThirdPartySaleLine dejaFacture = dossier(compte, 40_000);
        facture(compte.getTiersPayant(), false, List.of(dejaFacture));
        ThirdPartySaleLine aFacturer = dossier(compte, 15_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionAllService.createFactureEdition(parametres(ModeEditionEnum.ALL));
        viderLeCache();

        FactureTiersPayant facture = facturesDe(reponse.generationCode()).getFirst();
        assertEquals(1, facture.getFacturesDetails().size());
        assertEquals(aFacturer.getId(), facture.getFacturesDetails().getFirst().getId());
    }

    @Test
    @DisplayName("Un dossier déjà porté par une facture provisoire est repris par la définitive")
    void dossierSurFactureProvisoire() throws Exception {
        ClientTiersPayant compte = compte(tiersPayant("PROVISOIRE"));
        ThirdPartySaleLine surProvisoire = dossier(compte, 40_000);
        facture(compte.getTiersPayant(), true, List.of(surProvisoire));
        viderLeCache();

        FactureEditionResponse reponse = services.editionAllService.createFactureEdition(parametres(ModeEditionEnum.ALL));
        viderLeCache();

        FactureTiersPayant definitive = facturesDe(reponse.generationCode()).getFirst();
        assertEquals(1, definitive.getFacturesDetails().size(), "la définitive reprend ce que la provisoire portait");
        assertEquals(definitive.getId(), em.find(ThirdPartySaleLine.class, surProvisoire.getId()).getFactureTiersPayant().getId());
    }

    @Test
    @DisplayName("Une facture provisoire ne prend que les dossiers jamais facturés")
    void editionProvisoire() throws Exception {
        ClientTiersPayant compte = compte(tiersPayant("PROV 2"));
        ThirdPartySaleLine surProvisoire = dossier(compte, 40_000);
        facture(compte.getTiersPayant(), true, List.of(surProvisoire));
        ThirdPartySaleLine vierge = dossier(compte, 12_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionAllService.createFactureEdition(
            new EditionSearchParams(
                null,
                ModeEditionEnum.ALL,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                Set.of(),
                Set.of(),
                Set.of(),
                true,
                Set.of(),
                true,
                OrigineGeneration.MANUELLE
            )
        );
        viderLeCache();

        FactureTiersPayant provisoire = facturesDe(reponse.generationCode()).getFirst();
        assertTrue(provisoire.isFactureProvisoire());
        assertEquals(1, provisoire.getFacturesDetails().size());
        assertEquals(vierge.getId(), provisoire.getFacturesDetails().getFirst().getId());
    }

    @Test
    @DisplayName("L'édition par tiers payant ne retient que les organismes désignés")
    void editionParTiersPayantDesigne() throws Exception {
        TiersPayant vise = tiersPayant("VISE");
        TiersPayant ignore = tiersPayant("IGNORE");
        dossier(compte(vise), 20_000);
        dossier(compte(ignore), 30_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionByTiersPayantService.createFactureEdition(
            parametres(ModeEditionEnum.TIERS_PAYANT, Set.of(vise.getId()), Set.of(), Set.of())
        );
        viderLeCache();

        List<FactureTiersPayant> factures = facturesDe(reponse.generationCode());
        assertEquals(1, factures.size());
        assertEquals(vise.getId(), factures.getFirst().getTiersPayant().getId());
    }

    @Test
    @DisplayName("L'édition par catégorie ne retient que les organismes de ce type")
    void editionParCategorie() throws Exception {
        TiersPayant assurance = tiersPayant("ASSUREUR", TiersPayantCategorie.ASSURANCE);
        TiersPayant depot = tiersPayant("DEPOT VO", TiersPayantCategorie.DEPOT);
        dossier(compte(assurance), 20_000);
        dossier(compte(depot), 30_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionByTypeTiersPayantService.createFactureEdition(
            parametres(ModeEditionEnum.TYPE, Set.of(), Set.of(), Set.of(TiersPayantCategorie.DEPOT))
        );
        viderLeCache();

        List<FactureTiersPayant> factures = facturesDe(reponse.generationCode());
        assertEquals(1, factures.size());
        assertEquals(depot.getId(), factures.getFirst().getTiersPayant().getId());
    }

    @Test
    @DisplayName("L'édition par sélection de bons ne facture que les dossiers cochés")
    void editionParSelectionDeBons() throws Exception {
        ClientTiersPayant compte = compte(tiersPayant("SELECTION"));
        ThirdPartySaleLine choisi = dossier(compte, 20_000);
        ThirdPartySaleLine laisse = dossier(compte, 35_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionBySelectionBonsService.createFactureEdition(
            parametres(ModeEditionEnum.SELECTION_BON, Set.of(), Set.of(choisi.getId().getId()), Set.of())
        );
        viderLeCache();

        FactureTiersPayant facture = facturesDe(reponse.generationCode()).getFirst();
        assertEquals(1, facture.getFacturesDetails().size());
        assertNull(em.find(ThirdPartySaleLine.class, laisse.getId()).getFactureTiersPayant());
    }

    @Test
    @DisplayName("L'édition par groupe crée une facture porteuse et une facture par adhérent")
    void editionParGroupe() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE SANTE");
        TiersPayant premier = tiersPayant("ADHERENT A", TiersPayantCategorie.ASSURANCE, groupe);
        TiersPayant second = tiersPayant("ADHERENT B", TiersPayantCategorie.ASSURANCE, groupe);
        dossier(compte(premier), 30_000);
        dossier(compte(second), 70_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionByGroupTiersService.createFactureEdition(
            parametres(ModeEditionEnum.GROUP, Set.of(), Set.of(), Set.of())
        );
        viderLeCache();

        assertTrue(reponse.isGroup());
        List<FactureTiersPayant> factures = facturesDe(reponse.generationCode());
        assertEquals(3, factures.size(), "deux filles et leur porteuse");
        FactureTiersPayant porteuse = factures.stream().filter(f -> f.getGroupeTiersPayant() != null).findFirst().orElseThrow();
        assertEquals(groupe.getId(), porteuse.getGroupeTiersPayant().getId());
        assertNull(porteuse.getTiersPayant(), "la porteuse n'appartient à aucun organisme en particulier");
        assertEquals(2, factures.stream().filter(f -> f.getGroupeFactureTiersPayant() != null).count());
    }

    @Test
    @DisplayName("La facture porteuse cumule les montants de ses filles")
    void cumulSurLaFacturePorteuse() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE CUMUL");
        TiersPayant premier = tiersPayant("ADHERENT C", TiersPayantCategorie.ASSURANCE, groupe);
        TiersPayant second = tiersPayant("ADHERENT D", TiersPayantCategorie.ASSURANCE, groupe);
        dossier(compte(premier), 30_000);
        dossier(compte(second), 70_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionByGroupTiersService.createFactureEdition(
            parametres(ModeEditionEnum.GROUP, Set.of(), Set.of(), Set.of())
        );
        viderLeCache();

        FactureTiersPayant porteuse = facturesDe(reponse.generationCode())
            .stream()
            .filter(f -> f.getGroupeTiersPayant() != null)
            .findFirst()
            .orElseThrow();
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(porteuse.getMontantTtc()), "30 000 + 70 000");
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(porteuse.getMontantNet()));
    }

    @Test
    @DisplayName("Deux groupes donnent deux factures porteuses distinctes")
    void deuxGroupes() throws Exception {
        GroupeTiersPayant premierGroupe = groupe("GROUPE X");
        GroupeTiersPayant secondGroupe = groupe("GROUPE Y");
        dossier(compte(tiersPayant("ADHERENT E", TiersPayantCategorie.ASSURANCE, premierGroupe)), 10_000);
        dossier(compte(tiersPayant("ADHERENT F", TiersPayantCategorie.ASSURANCE, secondGroupe)), 20_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionByGroupTiersService.createFactureEdition(
            parametres(ModeEditionEnum.GROUP, Set.of(), Set.of(), Set.of())
        );
        viderLeCache();

        assertEquals(2, facturesDe(reponse.generationCode()).stream().filter(f -> f.getGroupeTiersPayant() != null).count());
        assertEquals(4, facturesDe(reponse.generationCode()).size());
    }

    @Test
    @DisplayName("L'origine de génération est reportée sur la facture")
    void origineDeGeneration() throws Exception {
        ClientTiersPayant compte = compte(tiersPayant("ORIGINE"));
        dossier(compte, 10_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionAllService.createFactureEdition(
            new EditionSearchParams(
                null,
                ModeEditionEnum.ALL,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                Set.of(),
                Set.of(),
                Set.of(),
                true,
                Set.of(),
                false,
                OrigineGeneration.AUTO
            )
        );
        viderLeCache();

        assertEquals(OrigineGeneration.AUTO, facturesDe(reponse.generationCode()).getFirst().getOrigineGeneration());
    }

    @Test
    @DisplayName("La facture retient la période éditée et la remise forfaitaire de l'organisme")
    void periodeEtRemiseForfaitaire() throws Exception {
        TiersPayant organisme = tiersPayant("REMISE");
        organisme.setRemiseForfaitaire(3);
        dossier(compte(organisme), 10_000);
        viderLeCache();

        FactureEditionResponse reponse = services.editionAllService.createFactureEdition(parametres(ModeEditionEnum.ALL));
        viderLeCache();

        FactureTiersPayant facture = facturesDe(reponse.generationCode()).getFirst();
        assertEquals(LocalDate.now().minusDays(1), facture.getDebutPeriode());
        assertEquals(LocalDate.now().plusDays(1), facture.getFinPeriode());
        assertEquals(3, facture.getRemiseForfetaire());
        assertEquals(utilisateur.getId(), facture.getUser().getId());
    }

    @Test
    @DisplayName("Le registre rend le service correspondant au mode demandé")
    void registreDesModesDEdition() {
        assertEquals(services.editionAllService, services.facturationServiceRegistry.getService(ModeEditionEnum.ALL));
        assertEquals(services.editionByGroupTiersService, services.facturationServiceRegistry.getService(ModeEditionEnum.GROUP));
        assertEquals(
            services.editionBySelectionBonsService,
            services.facturationServiceRegistry.getService(ModeEditionEnum.SELECTION_BON)
        );
    }

    // ===== outils =====

    private EditionSearchParams parametres(ModeEditionEnum mode) {
        return parametres(mode, Set.of(), Set.of(), Set.of());
    }

    private EditionSearchParams parametres(
        ModeEditionEnum mode,
        Set<Integer> tiersPayantIds,
        Set<Long> dossierIds,
        Set<TiersPayantCategorie> categories
    ) {
        return new EditionSearchParams(
            null,
            mode,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1),
            Set.of(),
            tiersPayantIds,
            dossierIds,
            true,
            categories,
            false,
            OrigineGeneration.MANUELLE
        );
    }
}
