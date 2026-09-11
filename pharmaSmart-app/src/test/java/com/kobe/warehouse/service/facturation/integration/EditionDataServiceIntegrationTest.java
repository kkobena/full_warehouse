package com.kobe.warehouse.service.facturation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.OrigineGeneration;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.facturation.dto.DossierFactureDto;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.FacturationKpiDto;
import com.kobe.warehouse.service.facturation.dto.InvoiceSearchParams;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * {@link com.kobe.warehouse.service.facturation.service.EditionDataService} sur un vrai PostgreSQL.
 *
 * <p>C'est le service que l'écran de facturation sollicite le plus : il liste les dossiers restant
 * à facturer, les factures déjà émises, leurs indicateurs, et il sait défaire une édition. Presque
 * tout y passe par une spécification Criteria ou une projection dédiée — rien qui s'observe hors
 * base.
 *
 * <p>La suppression mérite une attention particulière : effacer une facture doit rendre ses
 * dossiers à l'état « à facturer », sans quoi ils resteraient invisibles pour toujours, rattachés à
 * une facture disparue.
 */
@DisplayName("EditionDataService — données de l'écran de facturation sur PostgreSQL")
class EditionDataServiceIntegrationTest extends AbstractFacturationIntegrationTest {

    @Test
    @DisplayName("Les dossiers restant à facturer remontent avec leur assuré et leurs montants")
    void dossiersAFacturer() {
        TiersPayant organisme = tiersPayant("CNAM DATA");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine dossier = dossier(compte, 25_000);
        viderLeCache();

        List<DossierFactureDto> dossiers = services.editionDataService.getSales(parametres(), page()).getContent();

        assertEquals(1, dossiers.size());
        DossierFactureDto lu = dossiers.getFirst();
        assertEquals(dossier.getId().getId(), lu.getId());
        assertEquals(25_000, lu.getMontantBon());
        assertEquals(25_000, lu.getMontantVente());
        assertNotNull(lu.getAssuredCustomer(), "l'assuré vient de la vente rattachée au dossier");
        assertEquals(compte.getAssuredCustomer().getId(), lu.getAssuredCustomer().getId());
    }

    @Test
    @DisplayName("Un dossier déjà facturé sort de la liste des dossiers à facturer")
    void dossierDejaFactureExclu() {
        TiersPayant organisme = tiersPayant("EXCLU");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine facture = dossier(compte, 25_000);
        facture(organisme, false, List.of(facture));
        dossier(compte, 10_000);
        viderLeCache();

        List<DossierFactureDto> dossiers = services.editionDataService.getSales(parametres(), page()).getContent();

        assertEquals(1, dossiers.size());
        assertEquals(10_000, dossiers.getFirst().getMontantBon());
    }

    @Test
    @DisplayName("La liste des dossiers se restreint à l'organisme demandé")
    void dossiersFiltresParOrganisme() {
        TiersPayant vise = tiersPayant("VISE DATA");
        dossier(compte(vise), 15_000);
        dossier(compte(tiersPayant("AUTRE DATA")), 20_000);
        viderLeCache();

        List<DossierFactureDto> dossiers = services.editionDataService
            .getSales(parametres(Set.of(vise.getId()), Set.of()), page())
            .getContent();

        assertEquals(1, dossiers.size());
        assertEquals(15_000, dossiers.getFirst().getMontantBon());
    }

    @Test
    @DisplayName("La liste des dossiers se restreint au groupe demandé")
    void dossiersFiltresParGroupe() {
        GroupeTiersPayant groupe = groupe("GROUPE DATA");
        dossier(compte(tiersPayant("ADHERENT DATA", TiersPayantCategorie.ASSURANCE, groupe)), 12_000);
        dossier(compte(tiersPayant("HORS GROUPE")), 30_000);
        viderLeCache();

        List<DossierFactureDto> dossiers = services.editionDataService
            .getSales(parametres(Set.of(), Set.of(groupe.getId())), page())
            .getContent();

        assertEquals(1, dossiers.size());
        assertEquals(12_000, dossiers.getFirst().getMontantBon());
    }

    @Test
    @DisplayName("Les factures émises se relisent avec leur organisme et leurs montants")
    void listeDesFactures() {
        TiersPayant organisme = tiersPayant("LISTE");
        factureAvecMontant(organisme, 90_000, 30_000, InvoiceStatut.PARTIALLY_PAID);
        viderLeCache();

        List<FactureDto> factures = services.editionDataService.getInvoicies(recherche(), page()).getContent();

        assertEquals(1, factures.size());
        FactureDto facture = factures.getFirst();
        assertEquals(organisme.getFullName(), facture.getTiersPayantName());
        assertEquals(90_000, facture.getMontantNet());
        assertEquals(30_000, facture.getMontantRegle());
        assertEquals(InvoiceStatut.PARTIALLY_PAID, facture.getStatut());
    }

    @Test
    @DisplayName("Le filtre par statut ne rend que les factures dans cet état")
    void facturesFiltreesParStatut() {
        TiersPayant organisme = tiersPayant("STATUT DATA");
        factureAvecMontant(organisme, 40_000, 40_000, InvoiceStatut.PAID);
        factureAvecMontant(organisme, 50_000, 0, InvoiceStatut.NOT_PAID);
        viderLeCache();

        List<FactureDto> payees = services.editionDataService
            .getInvoicies(
                new InvoiceSearchParams(
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(1),
                    Set.of(),
                    Set.of(),
                    null,
                    Set.of(InvoiceStatut.PAID),
                    null,
                    TypeFacture.INDIVIDUAL
                ),
                page()
            )
            .getContent();

        assertEquals(1, payees.size());
        assertEquals(InvoiceStatut.PAID, payees.getFirst().getStatut());
    }

    @Test
    @DisplayName("Le sélecteur définitives / provisoires trie les factures")
    void facturesProvisoiresEtDefinitives() {
        TiersPayant organisme = tiersPayant("PROV DATA");
        ClientTiersPayant compte = compte(organisme);
        facture(organisme, true, List.of(dossier(compte, 10_000)));
        facture(organisme, false, List.of(dossier(compte, 20_000)));
        viderLeCache();

        List<FactureDto> provisoires = services.editionDataService
            .getInvoicies(rechercheAvecProvisoire(Boolean.TRUE), page())
            .getContent();
        List<FactureDto> definitives = services.editionDataService
            .getInvoicies(rechercheAvecProvisoire(Boolean.FALSE), page())
            .getContent();
        List<FactureDto> toutes = services.editionDataService.getInvoicies(rechercheAvecProvisoire(null), page()).getContent();

        assertEquals(1, provisoires.size());
        assertEquals(1, definitives.size());
        assertEquals(2, toutes.size(), "sans choix, les deux natures remontent");
    }

    @Test
    @DisplayName("Les factures groupées se relisent à part des factures individuelles")
    void facturesGroupees() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE FACTURES");
        dossier(compte(tiersPayant("ADHERENT LISTE 1", TiersPayantCategorie.ASSURANCE, groupe)), 40_000);
        dossier(compte(tiersPayant("ADHERENT LISTE 2", TiersPayantCategorie.ASSURANCE, groupe)), 60_000);
        viderLeCache();
        services.editionByGroupTiersService.createFactureEdition(editionDeToutLeMois(ModeEditionEnum.GROUP));
        viderLeCache();

        List<FactureDto> groupees = services.editionDataService.getGroupInvoicies(recherche(), page()).getContent();
        List<FactureDto> individuelles = services.editionDataService.getInvoicies(recherche(), page()).getContent();

        assertEquals(1, groupees.size(), "une seule porteuse pour le groupe");
        assertTrue(
            individuelles.isEmpty(),
            "les filles d'un groupe sortent de la liste individuelle : elles se règlent par leur porteuse"
        );
    }

    @Test
    @DisplayName("Supprimer une facture rend ses dossiers à l'état à facturer")
    void suppressionDUneFacture() {
        TiersPayant organisme = tiersPayant("SUPPRESSION");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine premier = dossier(compte, 20_000);
        ThirdPartySaleLine second = dossier(compte, 30_000);
        FactureTiersPayant facture = facture(organisme, false, List.of(premier, second));
        viderLeCache();

        services.editionDataService.deleteFacture(facture.getId());
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM facture_tiers_payant WHERE id = " + facture.getId().getId()));
        assertNull(em.find(ThirdPartySaleLine.class, premier.getId()).getFactureTiersPayant());
        assertNull(em.find(ThirdPartySaleLine.class, second.getId()).getFactureTiersPayant());
        assertEquals(2, services.editionDataService.getSales(parametres(), page()).getTotalElements(), "les dossiers reviennent");
    }

    @Test
    @DisplayName("Supprimer une facture de groupe emporte ses filles et libère leurs dossiers")
    void suppressionDUneFactureDeGroupe() throws Exception {
        GroupeTiersPayant groupe = groupe("GROUPE SUPPR");
        dossier(compte(tiersPayant("ADHERENT SUPPR 1", TiersPayantCategorie.ASSURANCE, groupe)), 30_000);
        dossier(compte(tiersPayant("ADHERENT SUPPR 2", TiersPayantCategorie.ASSURANCE, groupe)), 70_000);
        viderLeCache();
        FactureEditionResponse edition = services.editionByGroupTiersService.createFactureEdition(
            editionDeToutLeMois(ModeEditionEnum.GROUP)
        );
        viderLeCache();
        FactureTiersPayant porteuse = facturesDe(edition.generationCode())
            .stream()
            .filter(f -> f.getGroupeTiersPayant() != null)
            .findFirst()
            .orElseThrow();

        services.editionDataService.deleteFacture(porteuse.getId());
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM facture_tiers_payant"), "la porteuse et ses deux filles");
        assertEquals(
            0,
            compter("SELECT count(*) FROM third_party_sale_line WHERE facture_tiers_payant_id IS NOT NULL"),
            "aucun dossier ne reste rattaché à une facture effacée"
        );
    }

    @Test
    @DisplayName("Plusieurs factures se suppriment en un appel")
    void suppressionEnLot() {
        TiersPayant organisme = tiersPayant("LOT SUPPR");
        ClientTiersPayant compte = compte(organisme);
        FactureTiersPayant premiere = facture(organisme, false, List.of(dossier(compte, 10_000)));
        FactureTiersPayant seconde = facture(organisme, false, List.of(dossier(compte, 20_000)));
        viderLeCache();

        services.editionDataService.deleteFacture(Set.of(premiere.getId(), seconde.getId()));
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM facture_tiers_payant"));
        assertEquals(0, compter("SELECT count(*) FROM third_party_sale_line WHERE facture_tiers_payant_id IS NOT NULL"));
    }

    @Test
    @DisplayName("Une facture se relit avec ses dossiers")
    void lectureDUneFacture() {
        TiersPayant organisme = tiersPayant("LECTURE");
        ClientTiersPayant compte = compte(organisme);
        FactureTiersPayant facture = facture(organisme, false, List.of(dossier(compte, 20_000), dossier(compte, 5_000)));
        viderLeCache();

        var wrapper = services.editionDataService.getFacture(facture.getId());

        assertTrue(wrapper.isPresent());
        assertEquals(facture.getId(), wrapper.orElseThrow().getFactureItemId());
    }

    @Test
    @DisplayName("Une facture inconnue est signalée plutôt que rendue vide")
    void lectureDUneFactureInconnue() {
        FactureItemId inconnue = new FactureItemId(-1L, LocalDate.now());

        assertThrows(GenericError.class, () -> services.editionDataService.getFacture(inconnue));
    }

    @Test
    @DisplayName("Les factures d'une génération se retrouvent par son code")
    void facturesDUneGeneration() throws Exception {
        dossier(compte(tiersPayant("GENERATION")), 15_000);
        viderLeCache();
        FactureEditionResponse edition = services.editionAllService.createFactureEdition(
            editionDeToutLeMois(ModeEditionEnum.ALL)
        );
        viderLeCache();

        List<FactureTiersPayant> factures = services.editionDataService.getFactureTiersPayant(edition.generationCode(), false);

        assertEquals(1, factures.size());
        assertEquals(edition.generationCode(), factures.getFirst().getGenerationCode());
    }

    @Test
    @DisplayName("Les indicateurs somment le facturé, le réglé et ce qui reste à recouvrer")
    void indicateurs() {
        lenient().when(services.appConfigurationService.getDelaiReglement()).thenReturn(30);
        TiersPayant organisme = tiersPayant("KPI");
        factureAvecMontant(organisme, 100_000, 40_000, InvoiceStatut.PARTIALLY_PAID);
        factureAvecMontant(organisme, 60_000, 60_000, InvoiceStatut.PAID);
        viderLeCache();

        FacturationKpiDto kpi = services.editionDataService.getKpi(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1),
            null,
            null,
            TypeFacture.INDIVIDUAL,
            null
        );

        assertEquals(160_000L, kpi.totalFacture());
        assertEquals(100_000L, kpi.totalRegle());
        assertEquals(60_000L, kpi.totalRestant());
        assertEquals(2L, kpi.countFactures());
        assertEquals(1L, kpi.countImpayees(), "seule la facture partiellement réglée reste due");
        assertEquals(62.5d, kpi.tauxRecouvrement(), 0.01d);
    }

    @Test
    @DisplayName("Sans facture sur la période, les indicateurs sont à zéro plutôt qu'en erreur")
    void indicateursSansFacture() {
        lenient().when(services.appConfigurationService.getDelaiReglement()).thenReturn(30);
        viderLeCache();

        FacturationKpiDto kpi = services.editionDataService.getKpi(
            LocalDate.now().minusMonths(6),
            LocalDate.now().minusMonths(5),
            null,
            null,
            TypeFacture.INDIVIDUAL,
            null
        );

        assertEquals(0L, kpi.totalFacture());
        assertEquals(0d, kpi.tauxRecouvrement(), 0.001d, "aucune division par zéro");
    }

    @Test
    @DisplayName("Le détail de règlement d'une facture rend ses dossiers")
    void detailDeReglement() {
        TiersPayant organisme = tiersPayant("DETAIL REGLEMENT");
        ClientTiersPayant compte = compte(organisme);
        FactureTiersPayant facture = facture(organisme, false, List.of(dossier(compte, 20_000), dossier(compte, 8_000)));
        viderLeCache();

        var dossiers = services.editionDataService.findFactureReglementData(facture.getId(), page());

        assertEquals(2, dossiers.getTotalElements());
    }

    @Test
    @DisplayName("L'export Excel des factures rend un classeur non vide")
    void exportExcel() {
        lenient().when(services.appConfigurationService.getDelaiReglement()).thenReturn(30);
        factureAvecMontant(tiersPayant("EXCEL DATA"), 50_000, 10_000, InvoiceStatut.PARTIALLY_PAID);
        viderLeCache();

        byte[] classeur = services.editionDataService.exportInvoicesToExcel(recherche(), false);

        assertTrue(classeur.length > 0);
    }

    // ===== outils =====

    private Pageable page() {
        return PageRequest.of(0, 20);
    }

    private EditionSearchParams parametres() {
        return parametres(Set.of(), Set.of());
    }

    private EditionSearchParams parametres(Set<Integer> tiersPayantIds, Set<Integer> groupIds) {
        return new EditionSearchParams(
            null,
            ModeEditionEnum.ALL,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1),
            groupIds,
            tiersPayantIds,
            Set.of(),
            true,
            Set.of(),
            false,
            OrigineGeneration.MANUELLE
        );
    }

    private InvoiceSearchParams recherche() {
        return rechercheAvecProvisoire(null);
    }

    private InvoiceSearchParams rechercheAvecProvisoire(Boolean provisoire) {
        return new InvoiceSearchParams(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1),
            Set.of(),
            Set.of(),
            provisoire,
            Set.of(),
            null,
            TypeFacture.INDIVIDUAL
        );
    }

    /**
     * Une facture porteuse d'un dossier. La projection des factures joint les dossiers en interne
     * et en somme les montants : une facture vide serait comptée mais jamais rendue, et son montant
     * viendrait de nulle part.
     */
    private FactureTiersPayant factureAvecMontant(TiersPayant organisme, int montantNet, int montantRegle, InvoiceStatut statut) {
        FactureTiersPayant facture = facture(organisme, false, List.of(dossier(compte(organisme), montantNet)));
        facture.setMontantTtc(BigDecimal.valueOf(montantNet));
        facture.setMontantNet(BigDecimal.valueOf(montantNet));
        facture.setMontantRegle(montantRegle);
        facture.setStatut(statut);
        em.flush();
        return facture;
    }

    private EditionSearchParams editionDeToutLeMois(ModeEditionEnum mode) {
        return new EditionSearchParams(
            null,
            mode,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1),
            Set.of(),
            Set.of(),
            Set.of(),
            true,
            Set.of(),
            false,
            OrigineGeneration.MANUELLE
        );
    }
}
